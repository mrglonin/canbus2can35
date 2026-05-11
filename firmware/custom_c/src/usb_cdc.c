#include "usb_cdc.h"

#include <stddef.h>

#include <libopencm3/cm3/common.h>
#include <libopencm3/stm32/f1/gpio.h>
#include <libopencm3/stm32/otg_fs.h>
#include <libopencm3/stm32/rcc.h>
#include <libopencm3/usb/cdc.h>
#include <libopencm3/usb/usbd.h>

#include "board.h"
#include "ringbuf.h"

#define EP_DATA_OUT 0x01
#define EP_DATA_IN  0x82
#define EP_COMM_IN  0x83

static usbd_device *usbdev;
static uint8_t usbd_control_buffer[256];
static bool configured;
static usb_rx_fn_t rx_callback;
static ringbuf_t tx_rb;

static void usb_startup_delay(void)
{
	for (volatile uint32_t i = 0; i < 720000U; i++) {
		__asm volatile ("nop");
	}
}

struct cdcacm_functional_descriptors {
	struct usb_cdc_header_descriptor header;
	struct usb_cdc_call_management_descriptor call_mgmt;
	struct usb_cdc_acm_descriptor acm;
	struct usb_cdc_union_descriptor cdc_union;
} __attribute__((packed));

static const struct usb_device_descriptor dev_descr = {
	.bLength = USB_DT_DEVICE_SIZE,
	.bDescriptorType = USB_DT_DEVICE,
	.bcdUSB = 0x0200,
	.bDeviceClass = USB_CLASS_CDC,
	.bDeviceSubClass = 0,
	.bDeviceProtocol = 0,
	.bMaxPacketSize0 = 64,
	.idVendor = 0x0483,
	.idProduct = 0x5740,
	.bcdDevice = 0x0212,
	.iManufacturer = 1,
	.iProduct = 2,
	.iSerialNumber = 3,
	.bNumConfigurations = 1,
};

static const struct usb_endpoint_descriptor comm_endp[] = {{
	.bLength = USB_DT_ENDPOINT_SIZE,
	.bDescriptorType = USB_DT_ENDPOINT,
	.bEndpointAddress = EP_COMM_IN,
	.bmAttributes = USB_ENDPOINT_ATTR_INTERRUPT,
	.wMaxPacketSize = 16,
	.bInterval = 255,
}};

static const struct usb_endpoint_descriptor data_endp[] = {{
	.bLength = USB_DT_ENDPOINT_SIZE,
	.bDescriptorType = USB_DT_ENDPOINT,
	.bEndpointAddress = EP_DATA_OUT,
	.bmAttributes = USB_ENDPOINT_ATTR_BULK,
	.wMaxPacketSize = 64,
	.bInterval = 1,
}, {
	.bLength = USB_DT_ENDPOINT_SIZE,
	.bDescriptorType = USB_DT_ENDPOINT,
	.bEndpointAddress = EP_DATA_IN,
	.bmAttributes = USB_ENDPOINT_ATTR_BULK,
	.wMaxPacketSize = 64,
	.bInterval = 1,
}};

static const struct cdcacm_functional_descriptors cdcacm_func = {
	.header = {
		.bFunctionLength = sizeof(struct usb_cdc_header_descriptor),
		.bDescriptorType = CS_INTERFACE,
		.bDescriptorSubtype = USB_CDC_TYPE_HEADER,
		.bcdCDC = 0x0110,
	},
	.call_mgmt = {
		.bFunctionLength = sizeof(struct usb_cdc_call_management_descriptor),
		.bDescriptorType = CS_INTERFACE,
		.bDescriptorSubtype = USB_CDC_TYPE_CALL_MANAGEMENT,
		.bmCapabilities = 0,
		.bDataInterface = 1,
	},
	.acm = {
		.bFunctionLength = sizeof(struct usb_cdc_acm_descriptor),
		.bDescriptorType = CS_INTERFACE,
		.bDescriptorSubtype = USB_CDC_TYPE_ACM,
		.bmCapabilities = 0,
	},
	.cdc_union = {
		.bFunctionLength = sizeof(struct usb_cdc_union_descriptor),
		.bDescriptorType = CS_INTERFACE,
		.bDescriptorSubtype = USB_CDC_TYPE_UNION,
		.bControlInterface = 0,
		.bSubordinateInterface0 = 1,
	},
};

static const struct usb_interface_descriptor comm_iface[] = {{
	.bLength = USB_DT_INTERFACE_SIZE,
	.bDescriptorType = USB_DT_INTERFACE,
	.bInterfaceNumber = 0,
	.bAlternateSetting = 0,
	.bNumEndpoints = 1,
	.bInterfaceClass = USB_CLASS_CDC,
	.bInterfaceSubClass = USB_CDC_SUBCLASS_ACM,
	.bInterfaceProtocol = USB_CDC_PROTOCOL_AT,
	.iInterface = 0,
	.endpoint = comm_endp,
	.extra = &cdcacm_func,
	.extralen = sizeof(cdcacm_func),
}};

static const struct usb_interface_descriptor data_iface[] = {{
	.bLength = USB_DT_INTERFACE_SIZE,
	.bDescriptorType = USB_DT_INTERFACE,
	.bInterfaceNumber = 1,
	.bAlternateSetting = 0,
	.bNumEndpoints = 2,
	.bInterfaceClass = USB_CLASS_DATA,
	.bInterfaceSubClass = 0,
	.bInterfaceProtocol = 0,
	.iInterface = 0,
	.endpoint = data_endp,
}};

static const struct usb_interface ifaces[] = {{
	.num_altsetting = 1,
	.altsetting = comm_iface,
}, {
	.num_altsetting = 1,
	.altsetting = data_iface,
}};

static const struct usb_config_descriptor config = {
	.bLength = USB_DT_CONFIGURATION_SIZE,
	.bDescriptorType = USB_DT_CONFIGURATION,
	.wTotalLength = 0,
	.bNumInterfaces = 2,
	.bConfigurationValue = 1,
	.iConfiguration = 0,
	.bmAttributes = USB_CONFIG_ATTR_DEFAULT,
	.bMaxPower = 50,
	.interface = ifaces,
};

static const char *usb_strings[] = {
	"KIA CANBOX",
	"KIA CANBOX 2CAN35",
	"37FFDA054247303859412243",
};

static const struct usb_cdc_line_coding line_coding_default = {
	.dwDTERate = 115200,
	.bCharFormat = USB_CDC_1_STOP_BITS,
	.bParityType = USB_CDC_NO_PARITY,
	.bDataBits = 8,
};

static void tx_poll(void)
{
	uint8_t packet[64];
	uint16_t len;

	if (!configured || ringbuf_empty(&tx_rb)) {
		return;
	}

	uint16_t saved_tail = tx_rb.tail;
	len = ringbuf_read(&tx_rb, packet, sizeof(packet));
	if (len == 0) {
		return;
	}

	if (usbd_ep_write_packet(usbdev, EP_DATA_IN, packet, len) == 0) {
		tx_rb.tail = saved_tail;
	}
}

static void data_rx_cb(usbd_device *dev, uint8_t ep)
{
	(void)ep;
	uint8_t buf[64];
	int len = usbd_ep_read_packet(dev, EP_DATA_OUT, buf, sizeof(buf));

	if (len > 0 && rx_callback != NULL) {
		rx_callback(buf, (uint16_t)len);
	}
}

static int control_request(usbd_device *dev, struct usb_setup_data *req,
			   uint8_t **buf, uint16_t *len,
			   usbd_control_complete_callback *complete)
{
	(void)dev;
	(void)complete;

	switch (req->bRequest) {
	case USB_CDC_REQ_SET_CONTROL_LINE_STATE:
		return USBD_REQ_HANDLED;
	case USB_CDC_REQ_SET_LINE_CODING:
		return USBD_REQ_HANDLED;
	case 0x21:
		if (*len > sizeof(line_coding_default)) {
			*len = sizeof(line_coding_default);
		}
		*buf = (uint8_t *)&line_coding_default;
		return USBD_REQ_HANDLED;
	default:
		return USBD_REQ_NOTSUPP;
	}
}

static void set_config(usbd_device *dev, uint16_t w_value)
{
	(void)w_value;

	usbd_ep_setup(dev, EP_DATA_OUT, USB_ENDPOINT_ATTR_BULK, 64, data_rx_cb);
	usbd_ep_setup(dev, EP_DATA_IN, USB_ENDPOINT_ATTR_BULK, 64, NULL);
	usbd_ep_setup(dev, EP_COMM_IN, USB_ENDPOINT_ATTR_INTERRUPT, 16, NULL);

	usbd_register_control_callback(
		dev,
		USB_REQ_TYPE_CLASS | USB_REQ_TYPE_INTERFACE,
		USB_REQ_TYPE_TYPE | USB_REQ_TYPE_RECIPIENT,
		control_request);

	configured = true;
}

void usb_cdc_init(usb_rx_fn_t rx_fn)
{
	rx_callback = rx_fn;
	ringbuf_init(&tx_rb);

#if USE_STOCK_BEEP
	board_beep(1);
#endif
	rcc_periph_clock_enable(RCC_GPIOA);
	rcc_periph_clock_enable(RCC_AFIO);
	rcc_periph_clock_enable(RCC_OTGFS);
	rcc_periph_reset_pulse(RST_OTGFS);
	usb_startup_delay();

	board_usb_force_connect();
	usbdev = usbd_init(&otgfs_usb_driver, &dev_descr, &config,
			   usb_strings, 3, usbd_control_buffer,
			   sizeof(usbd_control_buffer));
	usbd_register_set_config_callback(usbdev, set_config);
	board_usb_force_connect();
	usbd_disconnect(usbdev, true);
	usb_startup_delay();
	usbd_disconnect(usbdev, false);
#if USE_STOCK_BEEP
	board_beep(2);
#endif
}

void usb_cdc_poll(void)
{
	usbd_poll(usbdev);
	tx_poll();
}

bool usb_cdc_ready(void)
{
	return configured;
}

void usb_cdc_write(const uint8_t *data, uint16_t len)
{
	(void)ringbuf_write(&tx_rb, data, len);
}

uint32_t usb_cdc_dropped(void)
{
	return tx_rb.dropped;
}

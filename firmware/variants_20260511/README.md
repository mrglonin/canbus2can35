# 2CAN35 Sportage v08 variants

Все варианты собраны без установки на адаптер. База одинакова:

- UID: `37 FF DA 05 42 47 30 38 59 41 22 43`
- версия: `04 35 00 08`
- mode1: штатная прошивка прогера v08
- mode2: штатный update
- mode1 USB/CANBOX не переписан; добавлены только проверенные хуки переключения режимов

## Что пробовать

0. `07_v08_mode1_canbox_cdc_can_uart_USB.bin`
   - новый правильный вариант под текущую цель;
   - mode1 остается штатной прошивкой прогера v08: canbox стартует сразу, без mode3 и без `gs_usb`;
   - USB-интерфейс не меняется: тот же stock CDC/proprietary протокол;
   - поверх штатного протокола добавлены команды `0x70..0x74` для CAN RX, UART RX mirror, UART TX и CAN TX;
   - UART RX сделан зеркалом штатного USART2 RX-handler, а не чтением `USART_DR` из USB-команды, чтобы не ломать родную UART-логику canbox;
   - ST-Link полный образ: `07_v08_mode1_canbox_cdc_can_uart_STLINK64K.bin`.

1. `01_v08_stock_mode1__mode3_gsusb_uart_direct_USB.bin`
   - основной вариант для теста;
   - mode3 сразу входит в `gs_usb + UART sideband`;
   - эмуляция: `cmd0 -> mode1`, `cmd1 -> update`, `cmd3 -> mode3`.

2. `02_v08_stock_mode1__mode3_gsusb_uart_resetreboot_USB.bin`
   - тот же `gs_usb + UART sideband`, но mode3 запускается через BKP magic + system reset;
   - нужен, если direct-вход на железе нестабилен;
   - в эмуляции `cmd3` не доходит до mode3 напрямую, потому что уходит в reset path.

3. `03_v08_stock_mode1__mode3_gsusb_pure_direct_USB.bin`
   - чистый `gs_usb` без UART sideband;
   - нужен для проверки, что CAN logger сам по себе поднимается стабильно.

4. `04_v08_stock_mode1__mode3_gsusb_pure_resetreboot_USB.bin`
   - чистый `gs_usb`, вход через reset path;
   - запасной вариант для проверки проблем старта USB.

5. `05_v08_stock_mode1__mode3_gsusb_uart_direct_G_RZ_HYK63_USB.bin`
   - экспериментальный вариант с заменой строки профиля;
   - отличие от v08 вне хуков только одно: `HYK-RZ-10-0001-VK` -> `G-RZ-HYK63`;
   - первым лучше не прошивать, пока не нужно тестировать именно отображение профиля.

## Проверка без установки

Проверено:

- decode/encode roundtrip для всех USB-пакетов;
- UID/version совпадают;
- mode1 отличается от v08 только в разрешенных местах: reset-vector и два mode-switch hook;
- mode3 vector валиден;
- dispatcher эмулируется для direct-вариантов.

Для `reset-reboot` вариантов прямой переход в mode3 в эмуляторе не считается ошибкой: там специально выполняется путь через backup register и системный reset.

Последний локальный отчет: `local_test_report.json`, результат `PASS` для всех 5 вариантов.

Для варианта `07` локально проверено отдельно:

- decode/encode roundtrip;
- UID `37 FF DA 05 42 47 30 38 59 41 22 43`;
- версия `04 35 00 08`;
- reset vector остался штатный `0x08006e3d`;
- USB hook уходит в sideband-диспетчер только для команд `0x70..0x74`, остальные команды передаются в родной обработчик;
- CAN RX hooks сначала вызывают родную функцию чтения CAN, потом отправляют копию в USB;
- UART RX hook сначала зеркалит байт в наш кольцевой буфер, потом вызывает родную функцию записи в штатный RX ring;
- UART mirror RAM инициализируется один раз при первом USB/UART входе, чтобы `available/dropped` не читались из мусорной RAM после перезапуска.

Проверка на живом адаптере после USB update:

```text
/dev/cu.usbmodemKIA1 появился
uart marker=b'UART' available=0 dropped=0
ack: bb a1 41 07 70 01 15
```

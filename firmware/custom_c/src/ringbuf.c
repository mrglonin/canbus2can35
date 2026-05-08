#include "ringbuf.h"

#include <stddef.h>

static uint16_t next_pos(uint16_t pos)
{
	return (uint16_t)((pos + 1U) & (RINGBUF_SIZE - 1U));
}

void ringbuf_init(ringbuf_t *rb)
{
	rb->head = 0;
	rb->tail = 0;
	rb->dropped = 0;
}

bool ringbuf_put(ringbuf_t *rb, uint8_t b)
{
	uint16_t next = next_pos(rb->head);

	if (next == rb->tail) {
		rb->dropped++;
		return false;
	}
	rb->buf[rb->head] = b;
	rb->head = next;
	return true;
}

uint16_t ringbuf_write(ringbuf_t *rb, const uint8_t *data, uint16_t len)
{
	uint16_t written = 0;

	for (uint16_t i = 0; i < len; i++) {
		if (!ringbuf_put(rb, data[i])) {
			break;
		}
		written++;
	}
	return written;
}

uint16_t ringbuf_read(ringbuf_t *rb, uint8_t *out, uint16_t max_len)
{
	uint16_t n = 0;

	while (rb->tail != rb->head && n < max_len) {
		out[n++] = rb->buf[rb->tail];
		rb->tail = next_pos(rb->tail);
	}
	return n;
}

bool ringbuf_empty(const ringbuf_t *rb)
{
	return rb->tail == rb->head;
}

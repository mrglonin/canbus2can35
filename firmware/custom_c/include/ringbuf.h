#pragma once

#include <stdbool.h>
#include <stdint.h>

#define RINGBUF_SIZE 8192U

typedef struct {
	uint8_t buf[RINGBUF_SIZE];
	volatile uint16_t head;
	volatile uint16_t tail;
	volatile uint32_t dropped;
} ringbuf_t;

void ringbuf_init(ringbuf_t *rb);
bool ringbuf_put(ringbuf_t *rb, uint8_t b);
uint16_t ringbuf_write(ringbuf_t *rb, const uint8_t *data, uint16_t len);
uint16_t ringbuf_read(ringbuf_t *rb, uint8_t *out, uint16_t max_len);
bool ringbuf_empty(const ringbuf_t *rb);

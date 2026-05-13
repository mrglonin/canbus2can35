# Raise / RZC UART

Эта папка хранит данные по Raise/RZC UART отдельно от Simple Soft.

Файлы:

- `raise_rzc_korea_uart_matrix.csv` - компактная матрица команд Raise/RZC.
- `raise_uart_full_worklist.csv` - полный worklist кадров для проверки.
- `programmer_firmware_uart_verified.csv` - связки, подтвержденные прошивкой автора или живыми фото/логами.
- `raise_uart_command_candidates.csv` - кандидаты для лабораторного подбора.
- `uart_command_coverage_analysis.csv` - покрытие команд по найденным источникам.
- `uart_command_verifications.jsonl` - ручные проверки.

Статус важен: `public_confirmed`, `local_confirmed`, `programmer_firmware...`
можно использовать как рабочую базу; `candidate` сначала проверяется в машине.

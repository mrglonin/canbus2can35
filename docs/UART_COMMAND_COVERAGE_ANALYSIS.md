# UART command coverage analysis

Сверка показывает, совпадает ли тестовый список UART TX с тем, что уже было извлечено из матриц, profile surfaces и quick-команд dashboard.

## Итог

- UART candidates: `183`
- Raise/RZC FD candidates: `119`
- Simple/2E candidates: `64`
- Coverage rows checked: `88`
- Covered rows: `88`
- Missing rows: `0`

## Покрытие по протоколу/команде

| protocol | cmd | candidates |
|---|---:|---:|
| `canbox_2e` | `0x20` | 39 |
| `canbox_2e` | `0x21` | 3 |
| `canbox_2e` | `0x22` | 3 |
| `canbox_2e` | `0x23` | 3 |
| `canbox_2e` | `0x24` | 8 |
| `canbox_2e` | `0x25` | 2 |
| `canbox_2e` | `0x81` | 2 |
| `canbox_2e` | `0x90` | 1 |
| `canbox_2e` | `0xA0` | 2 |
| `canbox_2e` | `0xA6` | 1 |
| `raise_rzc_fd` | `0x01` | 6 |
| `raise_rzc_fd` | `0x02` | 40 |
| `raise_rzc_fd` | `0x03` | 13 |
| `raise_rzc_fd` | `0x04` | 8 |
| `raise_rzc_fd` | `0x05` | 19 |
| `raise_rzc_fd` | `0x06` | 5 |
| `raise_rzc_fd` | `0x07` | 10 |
| `raise_rzc_fd` | `0x08` | 5 |
| `raise_rzc_fd` | `0x09` | 9 |
| `raise_rzc_fd` | `0x7D` | 2 |
| `raise_rzc_fd` | `0x7F` | 1 |
| `raise_rzc_fd` | `0xEE` | 1 |

## Missing

Нет пропусков: все строки из Raise matrix, external profile surfaces и dashboard quick-команд имеют хотя бы один TX-кандидат.

## Вывод

Список совпадает с уже известной базой по структуре команд: все известные `FD` команды и все кандидатные `2E` команды представлены в TX-списке. Это не означает, что каждая команда даст эффект на машине: рабочими считаются только строки, которые завтра получат verdict `works` в dashboard.

CSV с детальной сверкой: `data/uart_command_coverage_analysis.csv`.

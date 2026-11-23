CREATE TEMPORARY TABLE seed_class_schedule (
    id BIGINT PRIMARY KEY,
    scheduled_start DATETIME NOT NULL,
    duration_seconds INT NOT NULL
);

INSERT INTO seed_class_schedule (id, scheduled_start, duration_seconds)
SELECT
    c.id,
    TIMESTAMP(
        DATE_ADD(
            CURRENT_DATE,
            INTERVAL MOD(
                CASE c.id
                    WHEN 1 THEN 6
                    WHEN 2 THEN 0
                    WHEN 3 THEN 1
                END - WEEKDAY(CURRENT_DATE) + 7,
                7
            ) DAY
        ),
        TIME(c.start_date)
    ),
    TIMESTAMPDIFF(SECOND, c.start_date, c.end_date)
FROM classe c
JOIN type_classes t ON t.id = c.id_type_classe
WHERE c.id IN (1, 2, 3)
  AND t.name = CASE c.id
      WHEN 1 THEN 'Futbol'
      WHEN 2 THEN 'Basketball'
      WHEN 3 THEN 'Tennis'
  END
  AND DATE(c.start_date) < CURRENT_DATE;

UPDATE classe c
JOIN seed_class_schedule s ON s.id = c.id
SET c.start_date = CASE
        WHEN s.scheduled_start <= CURRENT_TIMESTAMP
            THEN DATE_ADD(s.scheduled_start, INTERVAL 7 DAY)
        ELSE s.scheduled_start
    END,
    c.end_date = DATE_ADD(
        CASE
            WHEN s.scheduled_start <= CURRENT_TIMESTAMP
                THEN DATE_ADD(s.scheduled_start, INTERVAL 7 DAY)
            ELSE s.scheduled_start
        END,
        INTERVAL s.duration_seconds SECOND
    );

DROP TEMPORARY TABLE seed_class_schedule;

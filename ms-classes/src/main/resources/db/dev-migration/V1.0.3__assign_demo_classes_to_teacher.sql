UPDATE classe
SET coach_id = 900002,
    coach_name = 'Profesor Demo'
WHERE id IN (1, 2, 3)
  AND coach_id IS NULL;

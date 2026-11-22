UPDATE plans
SET benefits = CASE name
    WHEN 'Basico' THEN CONCAT(
        'Acceso a instalaciones de entrenamiento', CHAR(10),
        'Clases grupales según disponibilidad', CHAR(10),
        'Vigencia de 7 días'
    )
    WHEN 'Normal' THEN CONCAT(
        'Acceso a instalaciones de entrenamiento', CHAR(10),
        'Reserva de clases grupales según disponibilidad', CHAR(10),
        'Vigencia de 25 días'
    )
    WHEN 'Anual' THEN CONCAT(
        'Acceso a instalaciones de entrenamiento', CHAR(10),
        'Reserva de clases grupales según disponibilidad', CHAR(10),
        'Vigencia de 365 días'
    )
END
WHERE name IN ('Basico', 'Normal', 'Anual')
  AND (benefits IS NULL OR TRIM(benefits) = '');

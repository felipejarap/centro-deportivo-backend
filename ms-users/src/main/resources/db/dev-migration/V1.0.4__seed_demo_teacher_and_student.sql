INSERT INTO `user` (
    id_user,
    username,
    paternal_surname,
    maternal_surname,
    email,
    phone,
    id_type_user,
    first_name,
    last_name,
    active,
    specialty,
    registration_date
)
SELECT
    900002,
    'profesor.demo@kronofit.local',
    'Kronofit',
    'Profesor',
    'profesor.demo@kronofit.local',
    '+56911111111',
    (SELECT id FROM type_user WHERE name = 'Entrenador' LIMIT 1),
    'Profesor',
    'Demo',
    TRUE,
    'Entrenamiento funcional',
    CURRENT_DATE
WHERE NOT EXISTS (
    SELECT 1
    FROM `user`
    WHERE id_user = 900002 OR username = 'profesor.demo@kronofit.local'
);

INSERT INTO `user` (
    id_user,
    username,
    paternal_surname,
    maternal_surname,
    email,
    phone,
    id_type_user,
    first_name,
    last_name,
    active,
    registration_date
)
SELECT
    900003,
    'estudiante.demo@kronofit.local',
    'Kronofit',
    'Estudiante',
    'estudiante.demo@kronofit.local',
    '+56922222222',
    (SELECT id FROM type_user WHERE name = 'Cliente' LIMIT 1),
    'Estudiante',
    'Demo',
    TRUE,
    CURRENT_DATE
WHERE NOT EXISTS (
    SELECT 1
    FROM `user`
    WHERE id_user = 900003 OR username = 'estudiante.demo@kronofit.local'
);

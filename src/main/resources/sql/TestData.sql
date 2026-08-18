WITH r AS (
    SELECT id FROM realm WHERE name = 'aether'
),
     new_permission AS (
INSERT INTO permission (id, realm_id, name, subject, subject_label, action, action_label, condition_template, condition_label, created_at)
SELECT gen_random_uuid()::text, r.id, 'punti-campionamento - lettura con condizione',
       'punti-campionamento', 'Punti di campionamento', 'read', 'Lettura',
       '{istatRegione:{{istatRegione}},istatComune:{{istatComune}},idAreraGestore:{{idAreraGestore}},prAutonoma:{{prAutonoma}}}',
       'Lettura filtrata per area', now()
FROM r
         RETURNING id
),
new_role AS (
INSERT INTO role (id, realm_id, name, level)
SELECT gen_random_uuid()::text, r.id, 'Regione', 10
FROM r
    RETURNING id
    ),
    link AS (
INSERT INTO role_permission (role_id, permission_id)
SELECT new_role.id, new_permission.id FROM new_role, new_permission
    )
INSERT INTO user_role (id, app_user_id, role_id, valid_from, valid_to, attributes)
SELECT gen_random_uuid()::text, u.id, new_role.id, now(), NULL,
       '[{"key":"istatRegione","type":"string","value":"[1,2,3,4,5,6,7,8,9,10]","isArray":true},{"key":"idAreraGestore","type":"string","value":"[22245,17190,1211,17134,841]","isArray":true}]'
FROM app_user u, new_role
WHERE u.username = 'vale' AND u.realm_id = (SELECT id FROM r);
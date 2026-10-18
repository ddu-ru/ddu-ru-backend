-- Android QA cleanup for MySQL 8.
-- Deletes only QA350-marked rows and rows owned by qa350-user-XX@dduru.test users.
-- Real users 1, 2, 3 and their profile, onboarding, survey, tendency, and preferences remain intact.

SET NAMES utf8mb4;
SET @qa_today = CURDATE();

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS qa350_cleanup_users;
CREATE TEMPORARY TABLE qa350_cleanup_users (
    user_id BIGINT PRIMARY KEY
);
INSERT INTO qa350_cleanup_users (user_id)
SELECT id
FROM users
WHERE email LIKE 'qa350-user-%@dduru.test';

DROP TEMPORARY TABLE IF EXISTS qa350_cleanup_posts;
CREATE TEMPORARY TABLE qa350_cleanup_posts (
    post_id BIGINT PRIMARY KEY
);
INSERT INTO qa350_cleanup_posts (post_id)
SELECT DISTINCT p.id
FROM posts p
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = p.user_id
WHERE qu.user_id IS NOT NULL
   OR p.title LIKE '[QA350] %'
   OR p.content LIKE '[QA350:%'
   OR JSON_CONTAINS(COALESCE(p.tags, JSON_ARRAY()), JSON_QUOTE('QA350'))
   OR EXISTS (
       SELECT 1
       FROM participations participation
       WHERE participation.post_id = p.id
         AND participation.message LIKE '[QA350:%'
   )
   OR EXISTS (
       SELECT 1
       FROM journeys j
       JOIN journey_posts jp ON jp.journey_id = j.id
       WHERE j.post_id = p.id
         AND jp.content LIKE '[QA350:JP:%'
   );

DROP TEMPORARY TABLE IF EXISTS qa350_cleanup_journeys;
CREATE TEMPORARY TABLE qa350_cleanup_journeys (
    journey_id BIGINT PRIMARY KEY
);
INSERT INTO qa350_cleanup_journeys (journey_id)
SELECT j.id
FROM journeys j
JOIN qa350_cleanup_posts qp ON qp.post_id = j.post_id;

DROP TEMPORARY TABLE IF EXISTS qa350_cleanup_rooms;
CREATE TEMPORARY TABLE qa350_cleanup_rooms (
    room_id BIGINT PRIMARY KEY
);
INSERT INTO qa350_cleanup_rooms (room_id)
SELECT DISTINCT cr.id
FROM chat_rooms cr
LEFT JOIN qa350_cleanup_posts qp ON qp.post_id = cr.post_id
LEFT JOIN qa350_cleanup_journeys qj ON qj.journey_id = cr.journey_id
LEFT JOIN chat_room_members crm ON crm.room_id = cr.id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = crm.user_id
WHERE qp.post_id IS NOT NULL
   OR qj.journey_id IS NOT NULL
   OR qu.user_id IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS qa350_cleanup_journey_posts;
CREATE TEMPORARY TABLE qa350_cleanup_journey_posts (
    journey_post_id BIGINT PRIMARY KEY
);
INSERT INTO qa350_cleanup_journey_posts (journey_post_id)
SELECT DISTINCT jp.id
FROM journey_posts jp
LEFT JOIN qa350_cleanup_journeys qj ON qj.journey_id = jp.journey_id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = jp.author_user_id
WHERE qj.journey_id IS NOT NULL
   OR qu.user_id IS NOT NULL
   OR jp.content LIKE '[QA350:JP:%';

DROP TEMPORARY TABLE IF EXISTS qa350_cleanup_batches;
CREATE TEMPORARY TABLE qa350_cleanup_batches (
    batch_id BIGINT PRIMARY KEY
);
INSERT INTO qa350_cleanup_batches (batch_id)
SELECT batch.id
FROM mate_recommendation_batches batch
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = batch.user_id
WHERE qu.user_id IS NOT NULL
   OR (batch.user_id IN (1, 2, 3) AND batch.recommendation_date = @qa_today);

-- Notifications have polymorphic resource IDs, so remove them before their resources.
DELETE n
FROM notifications n
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = n.recipient_user_id
WHERE qu.user_id IS NOT NULL
   OR n.body LIKE '[QA350:N:%';

DELETE mr
FROM mate_recommendations mr
LEFT JOIN qa350_cleanup_batches qb ON qb.batch_id = mr.batch_id
LEFT JOIN qa350_cleanup_posts qp ON qp.post_id = mr.post_id
WHERE qb.batch_id IS NOT NULL OR qp.post_id IS NOT NULL;

DELETE batch
FROM mate_recommendation_batches batch
JOIN qa350_cleanup_batches qb ON qb.batch_id = batch.id;

DELETE pass
FROM mate_recommendation_passes pass
LEFT JOIN qa350_cleanup_posts qp ON qp.post_id = pass.post_id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = pass.user_id
WHERE qp.post_id IS NOT NULL OR qu.user_id IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS qa350_cleanup_tickets;
CREATE TEMPORARY TABLE qa350_cleanup_tickets (
    ticket_id BIGINT PRIMARY KEY
);
INSERT INTO qa350_cleanup_tickets (ticket_id)
SELECT DISTINCT ticket.id
FROM super_host_tickets ticket
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = ticket.user_id
LEFT JOIN super_host_exposures exposure ON exposure.ticket_id = ticket.id
LEFT JOIN qa350_cleanup_posts qp ON qp.post_id = exposure.post_id
WHERE qu.user_id IS NOT NULL OR qp.post_id IS NOT NULL;

DELETE exposure
FROM super_host_exposures exposure
LEFT JOIN qa350_cleanup_tickets qt ON qt.ticket_id = exposure.ticket_id
LEFT JOIN qa350_cleanup_posts qp ON qp.post_id = exposure.post_id
WHERE qt.ticket_id IS NOT NULL OR qp.post_id IS NOT NULL;

DELETE ticket
FROM super_host_tickets ticket
JOIN qa350_cleanup_tickets qt ON qt.ticket_id = ticket.id;

-- last_read_message_id points back to chat_messages, so memberships go first.
DELETE crm
FROM chat_room_members crm
JOIN qa350_cleanup_rooms qr ON qr.room_id = crm.room_id;

DELETE cm
FROM chat_messages cm
JOIN qa350_cleanup_rooms qr ON qr.room_id = cm.room_id;

DELETE cr
FROM chat_rooms cr
JOIN qa350_cleanup_rooms qr ON qr.room_id = cr.id;

DELETE comment
FROM journey_post_comments comment
LEFT JOIN qa350_cleanup_journey_posts qjp ON qjp.journey_post_id = comment.journey_post_id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = comment.author_user_id
WHERE qjp.journey_post_id IS NOT NULL
   OR qu.user_id IS NOT NULL
   OR comment.content LIKE '[QA350:JC:%';

DELETE image
FROM journey_post_images image
JOIN qa350_cleanup_journey_posts qjp ON qjp.journey_post_id = image.journey_post_id;

DELETE jp
FROM journey_posts jp
JOIN qa350_cleanup_journey_posts qjp ON qjp.journey_post_id = jp.id;

DELETE schedule
FROM journey_schedules schedule
LEFT JOIN qa350_cleanup_journeys qj ON qj.journey_id = schedule.journey_id
WHERE qj.journey_id IS NOT NULL
   OR schedule.memo LIKE '[QA350:%';

DELETE label
FROM journey_member_role_labels label
JOIN journey_members member ON member.id = label.journey_member_id
LEFT JOIN qa350_cleanup_journeys qj ON qj.journey_id = member.journey_id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = member.user_id
WHERE qj.journey_id IS NOT NULL OR qu.user_id IS NOT NULL;

DELETE member
FROM journey_members member
LEFT JOIN qa350_cleanup_journeys qj ON qj.journey_id = member.journey_id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = member.user_id
WHERE qj.journey_id IS NOT NULL OR qu.user_id IS NOT NULL;

DELETE j
FROM journeys j
JOIN qa350_cleanup_journeys qj ON qj.journey_id = j.id;

DELETE report
FROM reports report
JOIN qa350_cleanup_posts qp ON qp.post_id = report.post_id;

DELETE report
FROM reports report
JOIN qa350_cleanup_users reporter ON reporter.user_id = report.user_id;

DELETE report
FROM reports report
JOIN qa350_cleanup_users reviewer ON reviewer.user_id = report.reviewer_id;

DELETE participation
FROM participations participation
LEFT JOIN qa350_cleanup_posts qp ON qp.post_id = participation.post_id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = participation.user_id
WHERE qp.post_id IS NOT NULL OR qu.user_id IS NOT NULL;

DELETE pl
FROM post_likes pl
LEFT JOIN qa350_cleanup_posts qp ON qp.post_id = pl.post_id
LEFT JOIN qa350_cleanup_users qu ON qu.user_id = pl.user_id
WHERE qp.post_id IS NOT NULL OR qu.user_id IS NOT NULL;

DELETE p
FROM posts p
JOIN qa350_cleanup_posts qp ON qp.post_id = p.id;

DELETE token
FROM user_fcm_tokens token
JOIN qa350_cleanup_users qu ON qu.user_id = token.user_id;

DELETE preference
FROM user_recommendation_destination_preferences preference
JOIN qa350_cleanup_users qu ON qu.user_id = preference.user_id;

DELETE available_date
FROM user_recommendation_available_dates available_date
JOIN qa350_cleanup_users qu ON qu.user_id = available_date.user_id;

DELETE tag
FROM survey_activity_tags tag
JOIN surveys survey ON survey.id = tag.survey_id
JOIN qa350_cleanup_users qu ON qu.user_id = survey.user_id;

DELETE survey
FROM surveys survey
JOIN qa350_cleanup_users qu ON qu.user_id = survey.user_id;

DELETE tendency
FROM travel_tendencies tendency
JOIN qa350_cleanup_users qu ON qu.user_id = tendency.user_id;

DELETE onboarding
FROM user_onboardings onboarding
JOIN qa350_cleanup_users qu ON qu.user_id = onboarding.user_id;

DELETE profile
FROM profiles profile
JOIN qa350_cleanup_users qu ON qu.user_id = profile.user_id;

DELETE u
FROM users u
JOIN qa350_cleanup_users qu ON qu.user_id = u.id;

COMMIT;

SELECT 'remaining_virtual_users' AS metric, COUNT(*) AS value
FROM users WHERE email LIKE 'qa350-user-%@dduru.test'
UNION ALL
SELECT 'remaining_qa_posts', COUNT(*)
FROM posts
WHERE title LIKE '[QA350] %'
   OR content LIKE '[QA350:%'
   OR JSON_CONTAINS(COALESCE(tags, JSON_ARRAY()), JSON_QUOTE('QA350'))
UNION ALL
SELECT 'real_users_preserved', COUNT(*)
FROM users WHERE id IN (1, 2, 3);

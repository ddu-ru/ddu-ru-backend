-- Clean-database baseline representing the schema after V32.
-- Older V migrations remain available for existing databases; new databases start here.
SET FOREIGN_KEY_CHECKS = 0;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `avatar_profiles` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `display_name` varchar(64) NOT NULL,
  `speech_bubble_text` varchar(200) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `description_line_1` text NOT NULL,
  `description_line_2` text NOT NULL,
  `description_line_3` text NOT NULL,
  `tags` json NOT NULL,
  `avatar_type` enum('TTUR_BANJJAK','TTUR_BEOMI','TTUR_BONGBONG','TTUR_DASOM','TTUR_GLIM','TTUR_HARAM','TTUR_LUNA','TTUR_MALLANG','TTUR_MARU','TTUR_MUDI','TTUR_POGUN','TTUR_POSEUL','TTUR_SODAM','TTUR_SPARK','TTUR_SWEET','TTUR_TTORANG') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1pr6hhkcsi9p4dbn3rfy4smlb` (`avatar_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bg_colors` (
  `display_order` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `hex_code` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `room_id` bigint NOT NULL,
  `sender_id` bigint DEFAULT NULL,
  `content` text NOT NULL,
  `message_type` enum('IMAGE','SYSTEM','TEXT') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_messages_room_id` (`room_id`),
  KEY `FKgiqeap8ays4lf684x7m0r2729` (`sender_id`),
  CONSTRAINT `FKgiqeap8ays4lf684x7m0r2729` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhalwepod3944695ji0suwoqb9` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_room_members` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_read_message_id` bigint DEFAULT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role` enum('GUEST','HOST') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKeatdjwpl171huu28pvsan941c` (`room_id`,`user_id`),
  KEY `FKnsrnerd4byjyp88kxfskt8kg6` (`last_read_message_id`),
  KEY `idx_chat_room_members_user_id` (`user_id`),
  CONSTRAINT `FKbemsjj4g0iny4xpkvj5rwj6ab` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKdvub8k7sypahkamqjaiokb44t` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`id`),
  CONSTRAINT `FKnsrnerd4byjyp88kxfskt8kg6` FOREIGN KEY (`last_read_message_id`) REFERENCES `chat_messages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_rooms` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `journey_id` bigint DEFAULT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `post_id` bigint DEFAULT NULL,
  `room_type` enum('GROUP','PRIVATE') NOT NULL,
  `status` enum('ACTIVE','CLOSED','DELETED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKix8aa8gaam8fu3p3ncuneea51` (`journey_id`),
  KEY `FK5a17nbflkx8cdvinwg1ipgxfx` (`post_id`),
  CONSTRAINT `FK5a17nbflkx8cdvinwg1ipgxfx` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FKpq1jfi3d6w83lag3m2bgp6fki` FOREIGN KEY (`journey_id`) REFERENCES `journeys` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `destinations` (
  `country_code` varchar(2) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(100) NOT NULL,
  `country_name` varchar(100) NOT NULL,
  `image` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journey_member_role_labels` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `journey_member_id` bigint NOT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `custom_role_label` varchar(10) DEFAULT NULL,
  `role_type` enum('CUSTOM','NAVIGATION','PHOTO','RESERVATION','SCHEDULE','TREASURER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_role_labels_member` (`journey_member_id`),
  CONSTRAINT `FK5aqtabyqds9pm2i6yo76q6wa2` FOREIGN KEY (`journey_member_id`) REFERENCES `journey_members` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journey_members` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) NOT NULL,
  `journey_id` bigint NOT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `removed_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `role` enum('HOST','MEMBER') NOT NULL,
  `status` enum('ACTIVE','LEFT','REMOVED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKl5r0rl79gsng9avnkqho3q4d` (`journey_id`,`user_id`),
  KEY `FKlguqll76gqkd621mrtn3mr8oj` (`user_id`),
  CONSTRAINT `FK9rjub9361al4yhexulsd1qv7w` FOREIGN KEY (`journey_id`) REFERENCES `journeys` (`id`),
  CONSTRAINT `FKlguqll76gqkd621mrtn3mr8oj` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journey_post_comments` (
  `is_deleted` bit(1) NOT NULL,
  `author_user_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `journey_post_id` bigint NOT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `content` varchar(300) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqsn7pwxsshulg8sds7vvnabys` (`author_user_id`),
  KEY `FKicoja0db6gg4nt0d52jdjmxx` (`journey_post_id`),
  CONSTRAINT `FKicoja0db6gg4nt0d52jdjmxx` FOREIGN KEY (`journey_post_id`) REFERENCES `journey_posts` (`id`),
  CONSTRAINT `FKqsn7pwxsshulg8sds7vvnabys` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journey_post_images` (
  `sort_order` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `journey_post_id` bigint NOT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `image_url` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_journey_post_images_post` (`journey_post_id`),
  CONSTRAINT `FKf2pwuk4we8shr5edo6cyowlh6` FOREIGN KEY (`journey_post_id`) REFERENCES `journey_posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journey_posts` (
  `is_deleted` bit(1) NOT NULL,
  `is_notice` bit(1) NOT NULL,
  `author_user_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `journey_id` bigint NOT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `content` varchar(300) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlt3oq103u03qqrvkkeqdq8e51` (`author_user_id`),
  KEY `FK5o805mcs4xcfdu9oltkkxyc4k` (`journey_id`),
  CONSTRAINT `FK5o805mcs4xcfdu9oltkkxyc4k` FOREIGN KEY (`journey_id`) REFERENCES `journeys` (`id`),
  CONSTRAINT `FKlt3oq103u03qqrvkkeqdq8e51` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journey_schedules` (
  `day_offset` int NOT NULL,
  `end_time` time(6) DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `start_time` time(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `journey_id` bigint NOT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `place_name` varchar(30) NOT NULL,
  `title` varchar(30) NOT NULL,
  `memo` varchar(100) DEFAULT NULL,
  `category` enum('ACTIVITY','CAFE','MEAL','REST','SHOPPING','SIGHTSEEING') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjkm66ovnbgjedo0268swt71h5` (`journey_id`),
  CONSTRAINT `FKjkm66ovnbgjedo0268swt71h5` FOREIGN KEY (`journey_id`) REFERENCES `journeys` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journeys` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `photo_url` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKecf21nldx3mtjo28fax3rfggp` (`post_id`),
  CONSTRAINT `FK9e054vf6v4wd7emrf05nsudu` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mate_recommendation_batches` (
  `recommendation_date` date NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `status` enum('COMPLETED','CREATED','EMPTY','FAILED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mate_recommendation_batches_user_date` (`user_id`,`recommendation_date`),
  KEY `idx_mate_recommendation_batches_date_status` (`recommendation_date`,`status`),
  CONSTRAINT `FK49iylb9tprp39qaxjldwhkcjm` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mate_recommendation_passes` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mate_recommendation_passes_user_post` (`user_id`,`post_id`),
  KEY `idx_mate_recommendation_passes_post` (`post_id`),
  CONSTRAINT `FK82v4ibr2diweyiy7jhhhwocoh` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FKmp19c8mrgvgoub1k7ostt7vl9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mate_recommendations` (
  `match_percentage` int NOT NULL,
  `recommendation_rank` int NOT NULL,
  `batch_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `caution_points` json NOT NULL,
  `match_reasons` json NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mate_recommendations_batch_rank` (`batch_id`,`recommendation_rank`),
  UNIQUE KEY `uk_mate_recommendations_batch_post` (`batch_id`,`post_id`),
  KEY `FKel5msf0t2wj6t2whcnopkxh08` (`post_id`),
  CONSTRAINT `FK9no5rsu3hvliyxe2cauut2yi8` FOREIGN KEY (`batch_id`) REFERENCES `mate_recommendation_batches` (`id`),
  CONSTRAINT `FKel5msf0t2wj6t2whcnopkxh08` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `is_read` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `recipient_user_id` bigint NOT NULL,
  `resource_id` bigint NOT NULL,
  `body` varchar(500) NOT NULL,
  `resource_type` enum('CHAT_ROOM','JOURNEY','JOURNEY_POST','MATCH','POST','SCHEDULE') NOT NULL,
  `type` enum('JOURNEY_NOTICE','MATCH_APPLIED','MATCH_APPROVED','POST_UPDATED','SCHEDULE_CANCELED','SCHEDULE_CREATED','SCHEDULE_UPDATED','TRIP_UPCOMING') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKt8ievafor22iuvg5sd4p7lhbk` (`recipient_user_id`),
  CONSTRAINT `FKt8ievafor22iuvg5sd4p7lhbk` FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `participations` (
  `approved_at` datetime(6) DEFAULT NULL,
  `contacted_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `message` varchar(500) DEFAULT NULL,
  `status` enum('APPROVED','CONTACTING','PENDING','REJECTED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3fe2g3c1p7rs437le1q18rk6e` (`post_id`,`user_id`),
  KEY `FK166yf958qjqf8uoslyuk9e19p` (`user_id`),
  CONSTRAINT `FK166yf958qjqf8uoslyuk9e19p` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKiwe5yrwq5jdy634qhm4j3i0i8` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `post_like_unique` (`user_id`,`post_id`),
  KEY `FKa5wxsgl4doibhbed9gm7ikie2` (`post_id`),
  CONSTRAINT `FKa5wxsgl4doibhbed9gm7ikie2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FKkgau5n0nlewg6o9lr4yibqgxj` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `end_date` date NOT NULL,
  `is_age_any` bit(1) NOT NULL DEFAULT b'0',
  `is_deleted` bit(1) NOT NULL,
  `like_count` int NOT NULL DEFAULT '0',
  `max_age` int DEFAULT NULL,
  `min_age` int DEFAULT NULL,
  `recruit_capacity` int NOT NULL,
  `recruit_count` int NOT NULL DEFAULT '1',
  `recruit_deadline` date DEFAULT NULL,
  `start_date` date NOT NULL,
  `view_count` int NOT NULL DEFAULT '0',
  `created_at` datetime(6) DEFAULT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `deleted_by` bigint DEFAULT NULL,
  `destination_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `photo_url` text,
  `tags` json DEFAULT NULL,
  `companion_type` enum('FULL','MEAL','PARTIAL','UNSPECIFIED') NOT NULL,
  `preferred_gender` enum('F','M','U') NOT NULL,
  `status` enum('CLOSED','OPEN') NOT NULL DEFAULT 'OPEN',
  PRIMARY KEY (`id`),
  KEY `FK68e4swrhvw4vvtvibno47rhbx` (`destination_id`),
  KEY `FK5lidm6cqbc7u4xhqpxm898qme` (`user_id`),
  CONSTRAINT `FK5lidm6cqbc7u4xhqpxm898qme` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK68e4swrhvw4vvtvibno47rhbx` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profiles` (
  `birthday` date DEFAULT NULL,
  `avatar_id` bigint DEFAULT NULL,
  `bg_color_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `nickname` varchar(14) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `bio` varchar(60) DEFAULT NULL,
  `uploaded_image_url` varchar(500) DEFAULT NULL,
  `gender` enum('F','M','U') DEFAULT NULL,
  `profile_image_type` enum('AVATAR','DEFAULT','UPLOADED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4ixsj6aqve5pxrbw2u0oyk8bb` (`user_id`),
  UNIQUE KEY `UKo3sh06ntflsc73hf6x6obygca` (`nickname`),
  UNIQUE KEY `UKbpki6nygo3ndvjpojbam4iamf` (`phone_number`),
  KEY `FKtlk7tgoyo5glil6ekxcgox14j` (`avatar_id`),
  KEY `FKxh13d2b9qwspg9b2gv0d9p5w` (`bg_color_id`),
  CONSTRAINT `FK410q61iev7klncmpqfuo85ivh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKtlk7tgoyo5glil6ekxcgox14j` FOREIGN KEY (`avatar_id`) REFERENCES `avatar_profiles` (`id`),
  CONSTRAINT `FKxh13d2b9qwspg9b2gv0d9p5w` FOREIGN KEY (`bg_color_id`) REFERENCES `bg_colors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `review_note` varchar(1000) DEFAULT NULL,
  `reason` enum('DUPLICATE','FALSE_INFO','INAPPROPRIATE','OTHER','PRIVACY_EXPOSURE','SCAM','SPAM_AD') NOT NULL,
  `status` enum('IN_REVIEW','RECEIVED','REJECTED','RESOLVED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKohnmvlokvkjg85rq6ww9vt6vb` (`post_id`,`user_id`),
  KEY `FKj3vqc51u40o0u5wgawwdtu6a7` (`reviewer_id`),
  KEY `FK2o32rer9hfweeylg7x8ut8rj2` (`user_id`),
  CONSTRAINT `FK2o32rer9hfweeylg7x8ut8rj2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKj3vqc51u40o0u5wgawwdtu6a7` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKneu1viyp671jjiwukyfv6dsy` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `super_host_exposures` (
  `created_at` datetime(6) DEFAULT NULL,
  `ended_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `ticket_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` enum('ACTIVE','CANCELLED','ENDED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhvi8p94utvte83n222rfxgspx` (`ticket_id`),
  KEY `FK4yovni8w0bfho4vohnbn82jpi` (`post_id`),
  KEY `FK7858nfq6aw4xtffuh7ig0y3l` (`user_id`),
  CONSTRAINT `FK4yovni8w0bfho4vohnbn82jpi` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FK7858nfq6aw4xtffuh7ig0y3l` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKh1sgd54b1bo5suut6i863x1e6` FOREIGN KEY (`ticket_id`) REFERENCES `super_host_tickets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `super_host_tickets` (
  `boost_duration_days` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `source` enum('ONBOARDING_SURVEY') NOT NULL,
  `status` enum('UNUSED','USED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKs6g205pokd2211r5o1wxpxyx1` (`user_id`),
  CONSTRAINT `FKs6g205pokd2211r5o1wxpxyx1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `survey_activity_tags` (
  `survey_id` bigint NOT NULL,
  `tag` enum('ACTIVITY','AMUSEMENT_PARK','EXHIBITION','FESTIVAL','FOOD','LEISURE','NATURE','SHOPPING','SIGHTSEEING') NOT NULL,
  KEY `FKc5nefvp0kh5p5quxfp59q3rcu` (`survey_id`),
  CONSTRAINT `FKc5nefvp0kh5p5quxfp59q3rcu` FOREIGN KEY (`survey_id`) REFERENCES `surveys` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `survey_question_options` (
  `code` int NOT NULL,
  `display_order` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `question_id` varchar(20) NOT NULL,
  `icon` varchar(50) DEFAULT NULL,
  `text` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_code` (`question_id`,`code`),
  CONSTRAINT `FK3lwo0mpmd8hvvw6ntpipa8rpo` FOREIGN KEY (`question_id`) REFERENCES `survey_questions` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `survey_questions` (
  `display_order` int NOT NULL,
  `max_select` int DEFAULT NULL,
  `min_select` int DEFAULT NULL,
  `required` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `modified_at` datetime(6) DEFAULT NULL,
  `question_id` varchar(20) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `text` varchar(500) NOT NULL,
  `type` enum('MULTI','SINGLE') NOT NULL,
  PRIMARY KEY (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `surveys` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `consumption_q1` enum('ADJUST_BUDGET','FLEX_OK') NOT NULL,
  `consumption_q2` enum('SAVE_TIME_TAXI','VALUE_TRANSPORT') NOT NULL,
  `consumption_q3` enum('INVEST_EXPERIENCE','VALUE_CHOICE') NOT NULL,
  `decision_q1` enum('DELEGATE_ROLE','LEAD_OR_ORGANIZE') NOT NULL,
  `decision_q2` enum('FOLLOW_OTHERS','PROPOSE_FIRST') NOT NULL,
  `decision_q3` enum('DRIVE_CONCLUSION','WAIT_AND_SEE') NOT NULL,
  `energy_q1` enum('PACKED_DAY','RELAXED_DAY') NOT NULL,
  `energy_q2` enum('BREAKFAST_SPRINT','BRUNCH_INSTEAD') NOT NULL,
  `energy_q3` enum('DO_NOTHING_OK','FILL_WITH_SPOTS') NOT NULL,
  `record_style` enum('EYES_FIRST','SHOOT_NOW') NOT NULL,
  `rhythm_q1` enum('IMPULSE_SIDE_TRIP','PLANNED_ROUTE') NOT NULL,
  `rhythm_q2` enum('PACK_EARLY','PACK_LAST_MINUTE') NOT NULL,
  `rhythm_q3` enum('ROUGH_LIST_ONLY','ROUTE_TIME_SET') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2uyqcnxjglu2wd6yfn8xos2ik` (`user_id`),
  CONSTRAINT `FKiydpdbdg90l5bl365gt67qgrn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `travel_tendencies` (
  `consumption_score` decimal(3,1) NOT NULL,
  `decision_score` decimal(3,1) NOT NULL,
  `energy_score` decimal(3,1) NOT NULL,
  `rhythm_score` decimal(3,1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `avatar_type` enum('TTUR_BANJJAK','TTUR_BEOMI','TTUR_BONGBONG','TTUR_DASOM','TTUR_GLIM','TTUR_HARAM','TTUR_LUNA','TTUR_MALLANG','TTUR_MARU','TTUR_MUDI','TTUR_POGUN','TTUR_POSEUL','TTUR_SODAM','TTUR_SPARK','TTUR_SWEET','TTUR_TTORANG') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKel276kqildk85dxxwpf785ill` (`user_id`),
  CONSTRAINT `FKap2gwf6083fjcp5s6bm09co5o` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_fcm_tokens` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token` varchar(500) NOT NULL,
  `device_type` enum('AOS','IOS') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKd9xccjm12q0mh4904hqd9o6rh` (`user_id`),
  CONSTRAINT `FKd9xccjm12q0mh4904hqd9o6rh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_onboardings` (
  `is_onboarding_completed` bit(1) NOT NULL,
  `is_profile_completed` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `survey_status` enum('COMPLETED','NOT_STARTED','SKIPPED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5w3gk2eu7y3fstwq4l5wjs1hk` (`user_id`),
  CONSTRAINT `FKltplsb7nf1cm9l8wyjabw738p` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_recommendation_available_dates` (
  `end_date` date NOT NULL,
  `start_date` date NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recommendation_available_dates_user_range` (`user_id`,`start_date`,`end_date`),
  CONSTRAINT `FKa24s9yi04a4q073xx5ir31dym` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_recommendation_destination_preferences` (
  `country_code` varchar(2) DEFAULT NULL,
  `preference_rank` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `destination_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `preference_type` enum('CITY','COUNTRY') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recommendation_destination_rank` (`user_id`,`preference_rank`),
  UNIQUE KEY `uk_recommendation_destination_country` (`user_id`,`preference_type`,`country_code`),
  UNIQUE KEY `uk_recommendation_destination_city` (`user_id`,`preference_type`,`destination_id`),
  KEY `FKkenhoux4jagvsr1x2v4yfmx2n` (`destination_id`),
  CONSTRAINT `FKkenhoux4jagvsr1x2v4yfmx2n` FOREIGN KEY (`destination_id`) REFERENCES `destinations` (`id`),
  CONSTRAINT `FKnrgrv5cwpf4t7duuo1npxox5c` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `user_recommendation_destination_preferences_chk_1` CHECK ((`preference_rank` between 1 and 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `notification_enabled` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `modified_at` datetime(6) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `oauth_id` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `oauth_type` enum('GOOGLE','KAKAO') NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

-- Indexes intentionally managed by migrations rather than JPA metadata.
CREATE INDEX idx_chat_messages_room_created ON chat_messages (room_id, created_at);
CREATE INDEX idx_chat_messages_room_id_id ON chat_messages (room_id, id);
CREATE INDEX idx_chat_room_members_room_id ON chat_room_members (room_id);
CREATE INDEX idx_journey_members_user_status ON journey_members (user_id, status);
CREATE INDEX idx_journey_members_journey_status ON journey_members (journey_id, status);
CREATE INDEX idx_journey_posts_journey_deleted_notice_created
    ON journey_posts (journey_id, is_deleted, is_notice, created_at, id);
CREATE INDEX idx_journey_post_comments_post_deleted_created
    ON journey_post_comments (journey_post_id, is_deleted, created_at, id);
CREATE INDEX idx_journey_schedules_journey_deleted_offset_time
    ON journey_schedules (journey_id, is_deleted, day_offset, start_time);
CREATE INDEX idx_notifications_recipient_visible_created
    ON notifications (recipient_user_id, deleted_at, created_at);
CREATE INDEX idx_super_host_tickets_user_status
    ON super_host_tickets (user_id, status);
CREATE INDEX idx_super_host_exposures_status_ended_at
    ON super_host_exposures (status, ended_at);
CREATE INDEX idx_super_host_exposures_post_status
    ON super_host_exposures (post_id, status);
CREATE INDEX idx_super_host_exposures_user_status
    ON super_host_exposures (user_id, status);

-- V31 is reserved for the popular-destination indexes and is included in this V32 snapshot.
CREATE INDEX idx_posts_deleted_created_destination
    ON posts (is_deleted, created_at, destination_id);
CREATE INDEX idx_posts_destination_deleted_status
    ON posts (destination_id, is_deleted, status);

-- Constraints that Hibernate schema generation does not reproduce completely.
ALTER TABLE chat_rooms
    ADD CONSTRAINT chk_chat_rooms_context CHECK (
        (room_type = 'PRIVATE' AND post_id IS NOT NULL AND journey_id IS NULL)
            OR (room_type = 'GROUP' AND post_id IS NULL AND journey_id IS NOT NULL)
    );

ALTER TABLE travel_tendencies
    ADD CONSTRAINT chk_travel_tendencies_rhythm_score CHECK (rhythm_score BETWEEN 0.0 AND 10.0),
    ADD CONSTRAINT chk_travel_tendencies_energy_score CHECK (energy_score BETWEEN 0.0 AND 10.0),
    ADD CONSTRAINT chk_travel_tendencies_consumption_score CHECK (consumption_score BETWEEN 0.0 AND 10.0),
    ADD CONSTRAINT chk_travel_tendencies_decision_score CHECK (decision_score BETWEEN 0.0 AND 10.0);

ALTER TABLE user_recommendation_destination_preferences
    DROP CHECK user_recommendation_destination_preferences_chk_1,
    ADD CONSTRAINT chk_recommendation_destination_rank CHECK (preference_rank BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_recommendation_destination_type CHECK (preference_type IN ('COUNTRY', 'CITY')),
    ADD CONSTRAINT chk_recommendation_destination_value CHECK (
        (preference_type = 'COUNTRY' AND country_code IS NOT NULL AND destination_id IS NULL)
            OR (preference_type = 'CITY' AND country_code IS NULL AND destination_id IS NOT NULL)
    );

ALTER TABLE user_recommendation_available_dates
    ADD CONSTRAINT chk_recommendation_available_dates_range CHECK (start_date <= end_date);

ALTER TABLE mate_recommendation_batches
    ADD CONSTRAINT chk_mate_recommendation_batches_status
        CHECK (status IN ('CREATED', 'COMPLETED', 'EMPTY', 'FAILED'));

ALTER TABLE mate_recommendations
    ADD CONSTRAINT chk_mate_recommendations_rank CHECK (recommendation_rank BETWEEN 1 AND 3),
    ADD CONSTRAINT chk_mate_recommendations_match CHECK (match_percentage BETWEEN 0 AND 100);

ALTER TABLE user_fcm_tokens
    ADD CONSTRAINT uk_user_fcm_tokens_token UNIQUE (token),
    ADD CONSTRAINT uk_user_fcm_tokens_user_device UNIQUE (user_id, device_type);

-- Preserve the cascade behavior declared by the original migrations.
ALTER TABLE journey_members
    DROP FOREIGN KEY FK9rjub9361al4yhexulsd1qv7w,
    ADD CONSTRAINT fk_journey_members_journey
        FOREIGN KEY (journey_id) REFERENCES journeys (id) ON DELETE CASCADE;

ALTER TABLE chat_rooms
    DROP FOREIGN KEY FKpq1jfi3d6w83lag3m2bgp6fki,
    ADD CONSTRAINT fk_chat_rooms_journey
        FOREIGN KEY (journey_id) REFERENCES journeys (id) ON DELETE CASCADE;

ALTER TABLE journey_posts
    DROP FOREIGN KEY FK5o805mcs4xcfdu9oltkkxyc4k,
    DROP FOREIGN KEY FKlt3oq103u03qqrvkkeqdq8e51,
    ADD CONSTRAINT fk_journey_posts_journey
        FOREIGN KEY (journey_id) REFERENCES journeys (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_journey_posts_author
        FOREIGN KEY (author_user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE journey_post_comments
    DROP FOREIGN KEY FKicoja0db6gg4nt0d52jdjmxx,
    DROP FOREIGN KEY FKqsn7pwxsshulg8sds7vvnabys,
    ADD CONSTRAINT fk_journey_post_comments_post
        FOREIGN KEY (journey_post_id) REFERENCES journey_posts (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_journey_post_comments_author
        FOREIGN KEY (author_user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE journey_schedules
    DROP FOREIGN KEY FKjkm66ovnbgjedo0268swt71h5,
    ADD CONSTRAINT fk_journey_schedules_journey
        FOREIGN KEY (journey_id) REFERENCES journeys (id) ON DELETE CASCADE;

ALTER TABLE journey_post_images
    DROP FOREIGN KEY FKf2pwuk4we8shr5edo6cyowlh6,
    ADD CONSTRAINT fk_journey_post_images_post
        FOREIGN KEY (journey_post_id) REFERENCES journey_posts (id) ON DELETE CASCADE;

ALTER TABLE survey_activity_tags
    DROP FOREIGN KEY FKc5nefvp0kh5p5quxfp59q3rcu,
    ADD CONSTRAINT fk_survey_activity_tags_survey
        FOREIGN KEY (survey_id) REFERENCES surveys (id) ON DELETE CASCADE;

ALTER TABLE user_fcm_tokens
    DROP FOREIGN KEY FKd9xccjm12q0mh4904hqd9o6rh,
    ADD CONSTRAINT fk_fcm_token_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_recommendation_destination_preferences
    DROP FOREIGN KEY FKnrgrv5cwpf4t7duuo1npxox5c,
    DROP FOREIGN KEY FKkenhoux4jagvsr1x2v4yfmx2n,
    ADD CONSTRAINT fk_recommendation_destination_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_recommendation_destination_destination
        FOREIGN KEY (destination_id) REFERENCES destinations (id) ON DELETE CASCADE;

ALTER TABLE user_recommendation_available_dates
    DROP FOREIGN KEY FKa24s9yi04a4q073xx5ir31dym,
    ADD CONSTRAINT fk_recommendation_available_dates_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE mate_recommendation_batches
    DROP FOREIGN KEY FK49iylb9tprp39qaxjldwhkcjm,
    ADD CONSTRAINT fk_mate_recommendation_batches_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE mate_recommendations
    DROP FOREIGN KEY FK9no5rsu3hvliyxe2cauut2yi8,
    DROP FOREIGN KEY FKel5msf0t2wj6t2whcnopkxh08,
    ADD CONSTRAINT fk_mate_recommendations_batch
        FOREIGN KEY (batch_id) REFERENCES mate_recommendation_batches (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_mate_recommendations_post
        FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE;

ALTER TABLE mate_recommendation_passes
    DROP FOREIGN KEY FKmp19c8mrgvgoub1k7ostt7vl9,
    ADD CONSTRAINT fk_mate_recommendation_passes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- Reference data required by profile, destination, and survey APIs.
INSERT INTO bg_colors (id, hex_code, display_order, created_at, modified_at)
VALUES
    (1, '0xFFF4B3BE', 1, NOW(6), NOW(6)),
    (2, '0xFFFCE29F', 2, NOW(6), NOW(6)),
    (3, '0xFFD8E6A8', 3, NOW(6), NOW(6)),
    (4, '0xFFCDF5FF', 4, NOW(6), NOW(6)),
    (5, '0xFFFAF8F4', 5, NOW(6), NOW(6)),
    (6, '0xFFF3CAD8', 6, NOW(6), NOW(6)),
    (7, '0xFFF8D495', 7, NOW(6), NOW(6)),
    (8, '0xFFC2DCAF', 8, NOW(6), NOW(6)),
    (9, '0xFFCDE8FF', 9, NOW(6), NOW(6)),
    (10, '0xFFDEDEDE', 10, NOW(6), NOW(6)),
    (11, '0xFFDCCDFC', 11, NOW(6), NOW(6)),
    (12, '0xFFF2C49B', 12, NOW(6), NOW(6)),
    (13, '0xFF9BC289', 13, NOW(6), NOW(6)),
    (14, '0xFFB8CCFF', 14, NOW(6), NOW(6)),
    (15, '0xFFB2B2B2', 15, NOW(6), NOW(6));

INSERT INTO destinations (country_code, country_name, city, image)
VALUES
    ('KR', '대한민국', '서울', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/seoul.jpg'),
    ('KR', '대한민국', '부산', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/busan.jpg'),
    ('KR', '대한민국', '제주', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/jeju.jpg'),
    ('KR', '대한민국', '인천', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/incheon.jpg'),
    ('KR', '대한민국', '강릉', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/gangneung.jpg'),
    ('KR', '대한민국', '속초', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/sokcho.jpg'),
    ('KR', '대한민국', '여수', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/yeosu.jpg'),
    ('KR', '대한민국', '경주', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/gyeongju.jpg'),
    ('KR', '대한민국', '전주', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/jeonju.jpg'),
    ('KR', '대한민국', '춘천', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/chuncheon.jpg'),
    ('KR', '대한민국', '포항', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/pohang.jpg'),
    ('KR', '대한민국', '대구', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/daegu.jpg'),
    ('KR', '대한민국', '광주', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/gwangju.jpg'),
    ('KR', '대한민국', '울산', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/ulsan.jpg'),
    ('KR', '대한민국', '대전', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/korea/daejeon.jpg'),
    ('JP', '일본', '도쿄', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/japan/tokyo.jpg'),
    ('JP', '일본', '오사카', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/japan/osaka.jpg'),
    ('JP', '일본', '교토', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/japan/kyoto.jpg'),
    ('JP', '일본', '후쿠오카', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/japan/fukuoka.jpg'),
    ('JP', '일본', '삿포로', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/japan/sapporo.jpg'),
    ('CN', '중국', '베이징', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/china/beijing.jpg'),
    ('CN', '중국', '충칭', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/china/chongqing.jpg'),
    ('CN', '중국', '상하이', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/china/shanghai.jpg'),
    ('CN', '중국', '항저우', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/china/hangzhou.jpg'),
    ('CN', '중국', '칭다오', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/china/qingdao.jpg'),
    ('CN', '중국', '청두', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/china/chengdu.jpg'),
    ('CN', '중국', '시안', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/china/xian.jpg'),
    ('TH', '태국', '방콕', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/thailand/bangkok.jpg'),
    ('TH', '태국', '푸켓', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/thailand/phuket.jpg'),
    ('TH', '태국', '치앙마이', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/thailand/chiangmai.jpg'),
    ('VN', '베트남', '다낭', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/vietnam/danang.jpg'),
    ('VN', '베트남', '하노이', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/vietnam/hanoi.jpg'),
    ('VN', '베트남', '호치민', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/vietnam/hochiminh.jpg'),
    ('VN', '베트남', '나트랑', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/vietnam/nhatrang.jpg'),
    ('SG', '싱가포르', '싱가포르', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/singapore/singapore.jpg'),
    ('TW', '대만', '타이베이', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/taiwan/taipei.jpg'),
    ('ID', '인도네시아', '발리', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/indonesia/bali.jpg'),
    ('US', '미국', '뉴욕', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/united-states/newyork.jpg'),
    ('US', '미국', '로스앤젤레스', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/united-states/losangeles.jpg'),
    ('US', '미국', '라스베이거스', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/united-states/lasvegas.jpg'),
    ('US', '미국', '샌프란시스코', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/united-states/sanfrancisco.jpg'),
    ('US', '미국', '하와이', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/united-states/hawaii.jpg'),
    ('FR', '프랑스', '파리', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/france/paris.jpg'),
    ('IT', '이탈리아', '로마', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/italy/rome.jpg'),
    ('IT', '이탈리아', '피렌체', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/italy/florence.jpg'),
    ('IT', '이탈리아', '베네치아', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/italy/venice.jpg'),
    ('ES', '스페인', '바르셀로나', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/spain/barcelona.jpg'),
    ('ES', '스페인', '마드리드', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/spain/madrid.jpg'),
    ('GB', '영국', '런던', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/united-kingdom/london.jpg'),
    ('CH', '스위스', '취리히', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/switzerland/zurich.jpg'),
    ('CH', '스위스', '인터라켄', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/switzerland/interlaken.jpg'),
    ('AU', '호주', '시드니', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/australia/sydney.jpg'),
    ('AU', '호주', '멜버른', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/australia/melbourne.jpg'),
    ('AE', '아랍에미리트', '두바이', 'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/destinations/uae/dubai.jpg');

INSERT INTO avatar_profiles
    (avatar_type, display_name, speech_bubble_text, description_line_1, description_line_2,
     description_line_3, image_url, tags, created_at, modified_at)
VALUES
    ('TTUR_POGUN', '포근', '계획 없이 떠나는 여행은 왠지 불안해요!',
     '즉흥적인 변수보다 미리 짜둔 계획 안에서 안정감을 느끼는 타입이에요.',
     '바쁘게 몰아치는 여행보다 여유로운 흐름을 선호하지만, 자연스럽게 그 흐름을 만들어가는 건 바로 본인이에요.',
     '예산도 꼼꼼히 따지면서 알차고 안정적인 여행을 설계하는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-pogun.png', '["꼼꼼함","여유로움","안정감"]', NOW(6), NOW(6)),
    ('TTUR_POSEUL', '포슬', '모두가 편안해야 내가 편해요!',
     '무리하지 않고 계획 안에서 편안하게 흘러가는 여행을 좋아하는 타입이에요.',
     '앞장서기보다는 주변 사람들과 자연스럽게 맞춰가며 분위기를 부드럽게 만들어줘요.',
     '가성비를 챙기면서도 조용하고 안정적인 여행을 즐기는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-poseul.png', '["따뜻함","배려심","조용함"]', NOW(6), NOW(6)),
    ('TTUR_TTORANG', '또랑', '동선이 곧 행복이에요, 낭비는 NO!',
     '동선부터 예산까지 꼼꼼하게 짜고 부지런하게 실행하는 전략형 타입이에요.',
     '하루를 알차게 채우는 걸 좋아하고, 자연스럽게 일정을 정리하고 이끌어가는 역할을 맡게 돼요.',
     '여행을 효율적으로 설계하고 실행하는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-ttorang.png', '["야무짐","전략적","부지런함"]', NOW(6), NOW(6)),
    ('TTUR_DASOM', '다솜', '알차게 움직이되, 함께가 먼저예요!',
     '계획적으로 움직이면서도 함께하는 사람들의 페이스에 맞춰주는 타입이에요.',
     '예산을 꼼꼼히 챙기면서 부지런히 움직이지만, 주도하기보다는 조화를 중요하게 생각해요.',
     '알차면서도 부드러운 여행을 만들어가는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-dasom.png', '["세심함","조화로움","성실함"]', NOW(6), NOW(6)),
    ('TTUR_MUDI', '무디', '분위기 좋은 곳엔 돈 아끼지 않아요!',
     '감성적인 장소와 특별한 분위기를 중요하게 여기는 타입이에요.',
     '여유롭게 즐기되, 여행의 흐름은 직접 만들어가고 싶어하는 편이에요.',
     '돈이 좀 들더라도 기억에 남는 경험을 선택하는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-mudi.png', '["감성적","안목","여유로움"]', NOW(6), NOW(6)),
    ('TTUR_SODAM', '소담', '좋은 순간은 천천히 스며드는 거예요!',
     '여행의 분위기와 감성을 소중히 여기며 천천히 스며드는 타입이에요.',
     '앞장서기보다는 그 순간의 분위기에 자연스럽게 녹아드는 걸 좋아해요.',
     '특별한 경험에는 기꺼이 투자하는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-sodam.png', '["따뜻함","낭만","세심함"]', NOW(6), NOW(6)),
    ('TTUR_SWEET', '스윗', '좋은 건 무조건 다 경험해봐야죠!',
     '좋은 경험을 절대 놓치지 않으려 부지런히 움직이는 적극적인 타입이에요.',
     '계획을 바탕으로 다양한 활동을 직접 이끌어가는 걸 좋아하고, 경험을 위해서라면 지출도 아끼지 않아요.',
     '여행을 꽉 채워 이끌어가는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-sweet.png', '["열정적","도전정신","적극적"]', NOW(6), NOW(6)),
    ('TTUR_BANJJAK', '반짝', '함께라면 어떤 경험도 빛나요!',
     '하루를 알차게 채우며 활동적으로 움직이는 걸 좋아하는 타입이에요.',
     '직접 나서기보다는 함께하는 사람들과의 조화를 중요하게 생각하며 경험에 적극적으로 투자해요.',
     '활기차면서도 배려심 있는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-banjjak.png', '["밝음","배려심","활발함"]', NOW(6), NOW(6)),
    ('TTUR_SPARK', '스파크', '계획이요? 현장에서 만들어가면 되죠!',
     '계획보다 현장의 느낌을 중요하게 여기고 새로운 경험을 찾아 즉흥적으로 움직이는 탐험형 타입이에요.',
     '예상치 못한 상황에서도 흔들리지 않고 여행을 신나게 이끌어가요.',
     '자유롭게 앞장서며 여행을 개척하는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-spark.png', '["탐험가","추진력","자유로움"]', NOW(6), NOW(6)),
    ('TTUR_LUNA', '루나', '새로운 건 일단 해보고 생각해요!',
     '정해진 틀 없이 자유롭게 움직이며 새로운 경험을 적극적으로 받아들이는 타입이에요.',
     '앞장서기보다는 즉흥적인 흐름 속에서 자연스럽게 어울리는 걸 좋아해요.',
     '어디서든 새로운 경험에 열려있는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-luna.png', '["개방적","도전정신","자유로움"]', NOW(6), NOW(6)),
    ('TTUR_GLIM', '글림', '감성 있는 순간은 내가 만들어요!',
     '감성적인 순간을 즉흥적으로 포착하며 여행의 분위기를 직접 만들어가는 타입이에요.',
     '서두르지 않고 천천히 움직이지만, 여행의 흐름은 본인이 이끌고 싶어해요.',
     '분위기 있는 여행을 즉흥적으로 설계하는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-glim.png', '["감성적","창의적","낭만"]', NOW(6), NOW(6)),
    ('TTUR_HARAM', '하람', '흘러가는 대로, 느끼는 대로예요!',
     '흘러가는 순간을 천천히 만끽하며 자연스럽게 주변과 어울리는 타입이에요.',
     '계획보다는 그 순간의 감성과 분위기에 몸을 맡기는 걸 좋아해요.',
     '즉흥적인 여행의 감성을 가장 잘 즐기는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-haram.png', '["자유로움","감성적","여유로움"]', NOW(6), NOW(6)),
    ('TTUR_MALLANG', '말랑', '부담 없이 가요, 근데 가성비는 챙겨요!',
     '빡빡한 계획 없이 느긋하게 즐기면서도 가성비는 놓치지 않는 타입이에요.',
     '분위기에 따라 자연스럽게 방향을 제시하며 부담 없는 여행을 만들어가요.',
     '여유롭지만 흐름을 슬쩍 잡아주는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-mallang.png', '["유연함","편안함","가성비"]', NOW(6), NOW(6)),
    ('TTUR_BONGBONG', '봉봉', '어디든 좋아요, 같이 가면 그게 최고!',
     '가볍고 편안하게 즉흥적으로 움직이며 예산도 슬기롭게 챙기는 타입이에요.',
     '주도하기보다는 흐름에 자연스럽게 녹아들며 주변 사람들과 편하게 어울려요.',
     '어디서든 부담 없이 함께할 수 있는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-bongbong.png', '["친화력","따뜻함","편안함"]', NOW(6), NOW(6)),
    ('TTUR_BEOMI', '범이', '일단 출발! 방법은 가면서 찾아요!',
     '상황에 맞게 빠르게 판단하고 가성비 있게 부지런히 움직이는 타입이에요.',
     '계획보다는 현장에서 직접 결론을 내리고 이끌어가는 걸 좋아해요.',
     '에너지 넘치게 여행을 주도하는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-beomi.png', '["추진력","순발력","활발함"]', NOW(6), NOW(6)),
    ('TTUR_MARU', '마루', '에너지 넘치게, 근데 예산은 지켜요!',
     '에너지 넘치게 즉흥적으로 움직이면서도 예산은 현명하게 챙기는 타입이에요.',
     '직접 이끌기보다는 빠르게 적응하며 어떤 상황에서도 분위기에 잘 녹아들어요.',
     '활발하게 움직이며 자연스럽게 어울리는 타입입니다.',
     'https://dduru26-v2.s3.ap-northeast-2.amazonaws.com/avatar/ttur-maru.png', '["에너지","적응력","가성비"]', NOW(6), NOW(6));

INSERT INTO survey_questions
    (question_id, display_order, type, required, min_select, max_select, text, image_url, created_at, modified_at)
VALUES
    ('rhythm_q1', 1, 'SINGLE', 1, 1, 1, '목적지로 향하는 길, 당신의 발걸음은 어디를 향하나요?', 'survey/rhythm_q1.png', NOW(6), NOW(6)),
    ('rhythm_q2', 2, 'SINGLE', 1, 1, 1, '여행 전, 짐 싸는 날!', 'survey/rhythm_q2.png', NOW(6), NOW(6)),
    ('rhythm_q3', 3, 'SINGLE', 1, 1, 1, '여행 일정 짜는 스타일은?', 'survey/rhythm_q3.png', NOW(6), NOW(6)),
    ('activity_tags', 4, 'MULTI', 1, 1, 3, '여행 가서 주로 뭐 하고 싶어?\n(최대 3개)', 'survey/activity_tags.png', NOW(6), NOW(6)),
    ('consumption_q1', 5, 'SINGLE', 1, 1, 1, '여행 예산이 살짝 오버됐다', 'survey/consumption_q1.png', NOW(6), NOW(6)),
    ('consumption_q2', 6, 'SINGLE', 1, 1, 1, '이동할 때는 어떻게 이동할까?', 'survey/consumption_q2.png', NOW(6), NOW(6)),
    ('consumption_q3', 7, 'SINGLE', 1, 1, 1, '여행 중 돈을 써야 할 선택지가 생겼다', 'survey/consumption_q3.png', NOW(6), NOW(6)),
    ('energy_q1', 8, 'SINGLE', 1, 1, 1, '여행지에서의 하루', 'survey/energy_q1.png', NOW(6), NOW(6)),
    ('energy_q2', 9, 'SINGLE', 1, 1, 1, '조식이 제공되는 호텔을 예약했다.', 'survey/energy_q2.png', NOW(6), NOW(6)),
    ('energy_q3', 10, 'SINGLE', 1, 1, 1, '하루 나에게 개인 자유 시간이 주어졌다.', 'survey/energy_q3.png', NOW(6), NOW(6)),
    ('decision_q1', 11, 'SINGLE', 1, 1, 1, '여행 준비할 때 역할을 나눈다면?', 'survey/decision_q1.png', NOW(6), NOW(6)),
    ('decision_q2', 12, 'SINGLE', 1, 1, 1, '의견을 모으거나 정해야 하는 상황에서 나는?', 'survey/decision_q2.png', NOW(6), NOW(6)),
    ('decision_q3', 13, 'SINGLE', 1, 1, 1, '단톡방에서 다들 눈치 보는 중…', 'survey/decision_q3.png', NOW(6), NOW(6)),
    ('record_style', 14, 'SINGLE', 1, 1, 1, '여행 중 사람/풍경/음식 등 사진이 예쁘게 나올 것 같은 순간을 만났다', 'survey/record_style.png', NOW(6), NOW(6));

INSERT INTO survey_question_options
    (question_id, display_order, code, icon, text, created_at, modified_at)
VALUES
    ('rhythm_q1', 1, 1, '✨', '최단 거리가 최고! 정해진 루트대로 다음 일정으로', NOW(6), NOW(6)),
    ('rhythm_q1', 2, 2, '🌈', '어? 저기 예쁜데? 가는 길에 궁금한 곳이 생기면 일단 들렀다 가자!', NOW(6), NOW(6)),
    ('rhythm_q2', 1, 1, '📋', '캐리어는 3일 전부터 오픈… 미리미리 완벽 준비', NOW(6), NOW(6)),
    ('rhythm_q2', 2, 2, '🔥', '여행 전날 밤? 그때 몰아서 싸지 뭐~', NOW(6), NOW(6)),
    ('rhythm_q3', 1, 1, '📅', '동선이랑 시간은 어느 정도 정해둬야 마음이 편해~', NOW(6), NOW(6)),
    ('rhythm_q3', 2, 2, '📍', '대충 가고 싶은 곳만 정해두고, 나머진 현장 느낌대로!', NOW(6), NOW(6)),
    ('activity_tags', 1, 1, '🏛️', '관광', NOW(6), NOW(6)),
    ('activity_tags', 2, 2, '⚽️', '관람', NOW(6), NOW(6)),
    ('activity_tags', 3, 3, '🌳', '자연 탐방', NOW(6), NOW(6)),
    ('activity_tags', 4, 4, '🍖', '맛집 탐방', NOW(6), NOW(6)),
    ('activity_tags', 5, 5, '🛍️', '쇼핑', NOW(6), NOW(6)),
    ('activity_tags', 6, 6, '🏖️', '휴양', NOW(6), NOW(6)),
    ('activity_tags', 7, 7, '🏄🏻‍♂️', '액티비티', NOW(6), NOW(6)),
    ('activity_tags', 8, 8, '🎡', '놀이공원', NOW(6), NOW(6)),
    ('activity_tags', 9, 9, '💃', '페스티벌', NOW(6), NOW(6)),
    ('consumption_q1', 1, 1, '📊', '잠깐… 지출 조정 타임. 예산에 맞춰야지', NOW(6), NOW(6)),
    ('consumption_q1', 2, 2, '💳', '여행인데 이 정도 FLEX는 괜찮지~', NOW(6), NOW(6)),
    ('consumption_q2', 1, 1, '🚇', '여행은 교통도 체험이지~ 가성비 이동!', NOW(6), NOW(6)),
    ('consumption_q2', 2, 2, '🚕', '시간 아끼는 게 곧 돈이야. 바로 택시!', NOW(6), NOW(6)),
    ('consumption_q3', 1, 1, '📊', '굳이 돈 안 써도 즐길 방법 많지! 가성비로 간다', NOW(6), NOW(6)),
    ('consumption_q3', 2, 2, '✨', '여행 추억은 이런 데서 생기지. 경험에 투자!', NOW(6), NOW(6)),
    ('energy_q1', 1, 1, '🐌', '카페-산책 … 여유롭게 즐기자 ~', NOW(6), NOW(6)),
    ('energy_q1', 2, 2, '🌅', '하루에 최대한 많이 다보자! 지금 아니면 언제 오겠어?', NOW(6), NOW(6)),
    ('energy_q2', 1, 1, '🙅🏻‍♀️', '오픈런은 무리… 느지막이 일어나서 근처 브런치 카페나 갈까?', NOW(6), NOW(6)),
    ('energy_q2', 2, 2, '🏃', '1등으로 입장! 조식 빨리 먹고 남들보다 먼저 관광지로 출발~', NOW(6), NOW(6)),
    ('energy_q3', 1, 1, '🌿', '아무것도 안 하는 날도 필요해', NOW(6), NOW(6)),
    ('energy_q3', 2, 2, '📍', '근처 명소 검색해서 바로 채우기!', NOW(6), NOW(6)),
    ('decision_q1', 1, 1, '🙂', '나는 맡겨주는 역할 하는 게 편해', NOW(6), NOW(6)),
    ('decision_q1', 2, 2, '📋', '자연스럽게 총괄 또는 정리하는 편!', NOW(6), NOW(6)),
    ('decision_q2', 1, 1, '👍', '난 다 좋아~ 따라갈게!', NOW(6), NOW(6)),
    ('decision_q2', 2, 2, '🗣️', '내가 먼저 "이렇게 해볼까?" 하고 제안한다.', NOW(6), NOW(6)),
    ('decision_q3', 1, 1, '😅', '나도 일단 말 안 하고 흐름 타기', NOW(6), NOW(6)),
    ('decision_q3', 2, 2, '🚀', '답답해! 내가 정리해서 결론 낸다', NOW(6), NOW(6)),
    ('record_style', 1, 1, '👀', '카메라는 잠시 넣어두고… 눈에 먼저 저장', NOW(6), NOW(6)),
    ('record_style', 2, 2, '📸', '잠깐만!! 이건 인생샷 각이다. 바로 찍어야지', NOW(6), NOW(6));

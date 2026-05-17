-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: booknest_auth
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  `provider` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (11,'2026-04-14 17:34:52.067192','divyanshpandey996@gmail.com','Divyansh Pandey','9109526982','$2a$10$Uzk3AjT4XFD664pCehvwYeeN7AP3dCZ2/fqf5x41BodPj4o0HLlVG','https://lh3.googleusercontent.com/a/ACg8ocKaScdsvi3zgoMwt3tkfiK80Q-xIN7Hyodv5Wbb8Z4B3PVUkkE=s96-c','LOCAL','ADMIN'),(12,'2026-04-15 00:45:34.389462','dishagujar1@gmail.com','Disha Gujar','9109526982','$2a$10$UId/GXFkiClryRkP1beZZuKZArLvmY2tcjKblcNBD5d5fGG5.nzQi',NULL,'LOCAL','CUSTOMER'),(13,'2026-04-15 10:31:20.825443','divyanshpandey2401@gmail.com','Divyansh Pandey',NULL,'$2a$10$Zf6p6TYZiMNxcCoXDNaxWetQsvQrvdmNPwjiKR/SJR9ZYpD.dtZiK','https://lh3.googleusercontent.com/a/ACg8ocJ-7pHInYEtFbVn77gIu6UKBqjSTlAvhbm2g3vc6SkX5vsFqQ9-fw=s96-c','GOOGLE','CUSTOMER'),(14,'2026-04-15 10:46:46.967385','shivankpandey2000@gmail.com','SHIVANK PANDEY',NULL,'GOOGLE_OAUTH_NO_PASSWORD','https://lh3.googleusercontent.com/a/ACg8ocI2GEGckLsq2KWLHT5ufqAZhb3T8BJE2bREpP1xOuyBGLeqJHtR=s96-c','GOOGLE','CUSTOMER'),(15,'2026-04-15 10:56:19.624947','divyanshpandey4002@gmail.com','Vishal Bhakre','9109526982','$2a$10$UW3lkGh7qq6LhFKoCnwTXehrqk/jztnUZcLXyPvdp0Hk4F0Dzpchm','https://lh3.googleusercontent.com/a/ACg8ocIsENMUqpTq1-X3Nt6PhaZT4-bVysy5PsbxArBsD_UzP-qc6W4=s96-c','LOCAL','CUSTOMER'),(17,'2026-04-15 13:11:12.720017','teamshivank@gmail.com','Anil Shukla','9109526982','$2a$10$IpG6eyt.byWD0rP14z9gBOTY7e6n7AJjDDEQWugep5CfPSQUCdbhK','https://lh3.googleusercontent.com/a/ACg8ocKzS6JoqKhEyVg5QCK0IYgmq3bJDhoJU-ZqgtsHzG80i_9Psw=s96-c','LOCAL','CUSTOMER'),(18,'2026-04-16 08:01:50.177299','mishrapriyanshu436@gmail.com','Priyansh Mishra','9302584377','$2a$10$bCK6E56Sfxen7mG/Csl4VeEb4cNR1c2BFxenV/ZKQ3Q5LE2jYNl12',NULL,'LOCAL','CUSTOMER'),(19,'2026-04-21 19:16:48.835856','divyanshpandeynhk@gmail.com','Abhiraj Singh','9109526982','$2a$10$F9XK7HlH0lDCQzCnYbgSL.GBz64qLbCYEScYqgsj51m/mswXl2eeu',NULL,'LOCAL','CUSTOMER'),(20,'2026-04-22 04:07:12.240186','testauth2425@gmail.com','Divyansh Pandey','9109526982','$2a$10$XSdcTUm6sIJf1agNY04I7enfrtaPbWz2QAgUupguDiQqi7/865.Du',NULL,'LOCAL','CUSTOMER'),(21,'2026-04-22 04:10:55.031682','shankar69great@gmail.com','Shankar Sir','9109526982','$2a$10$mQAWLLQmI00lrtq4n4VsQuhcgbkR1Ns5MiCsozA1IRlybsnLugNIi',NULL,'LOCAL','CUSTOMER'),(22,'2026-05-03 17:07:42.445945','groundf.news@gmail.com','Sudeep tripathi','9109526982','$2a$10$jpQ2Cpf9lJdyKGQXC8i4aOQbUt02.BS13mOXo4rGVzlBQpLnsYpUq',NULL,'LOCAL','CUSTOMER'),(23,'2026-05-13 08:56:08.139339','anujvishwakarma9827@gmail.com','Anuj Vishwakarma','9109526982','$2a$10$DqNECXY8abqtFN45m6XXJeuxFRr4ngU2y0.vKKfEKrRc/hp/VGyYa',NULL,'LOCAL','CUSTOMER');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14 15:19:51

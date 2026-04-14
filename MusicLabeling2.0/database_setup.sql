-- Crea il database
CREATE DATABASE IF NOT EXISTS musicLabelling;
USE musicLabelling;

-- Tabella users
CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  is_admin BOOLEAN DEFAULT FALSE
);

-- Tabella songs (senza user_id diretto, la relazione è in user_songs)
CREATE TABLE IF NOT EXISTS songs (
  song_id INT AUTO_INCREMENT PRIMARY KEY,
  song_name VARCHAR(100) NOT NULL,
  artist VARCHAR(100),
  file_path VARCHAR(255)
);

-- Tabella user_songs (quali utenti possono annotare quali canzoni)
CREATE TABLE IF NOT EXISTS user_songs (
  user_id INT NOT NULL,
  song_id INT NOT NULL,
  PRIMARY KEY (user_id, song_id),
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(song_id) ON DELETE CASCADE
);

-- Tabella labels (dizionario dei tipi di label disponibili)
CREATE TABLE IF NOT EXISTS labels (
  label_id INT AUTO_INCREMENT PRIMARY KEY,
  label_name VARCHAR(100) NOT NULL
);

-- Tabella user_song_labels (le annotazioni effettive: chi ha messo quale label su quale canzone e quando)
CREATE TABLE IF NOT EXISTS user_song_labels (
  user_song_label_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  song_id INT NOT NULL,
  label_id INT NOT NULL,
  timing INT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (song_id) REFERENCES songs(song_id) ON DELETE CASCADE,
  FOREIGN KEY (label_id) REFERENCES labels(label_id) ON DELETE CASCADE
);

-- Utenti seed (password: admin123 e password1, hashate con BCrypt cost 10)
-- Per aggiungere nuovi utenti: generare l'hash con HashPassword.java, poi:
-- INSERT INTO users (username, password, is_admin) VALUES ('nome', '<hash>', FALSE);
INSERT INTO users (username, password, is_admin) VALUES ('admin', '$2a$10$x/XqruOfTahKODMMSN6auexhz5MCxbiQKH8ffz/u5oM8xFXaoK.h.', TRUE);
INSERT INTO users (username, password, is_admin) VALUES ('user1', '$2a$10$8enm13U5TkB8CVcjjco8Q.OYfLU544sUNoIo24.QW.5MfVwx6NVeO', FALSE);

-- Label seed (tipi di annotazione disponibili nel dropdown — modifica come vuoi)
INSERT INTO labels (label_name) VALUES ('Delighted/Excited');
INSERT INTO labels (label_name) VALUES ('Happy/Pleased');
INSERT INTO labels (label_name) VALUES ('Relaxed/Serene');
INSERT INTO labels (label_name) VALUES ('Tired/Calm');
INSERT INTO labels (label_name) VALUES ('Depressed/Bored');
INSERT INTO labels (label_name) VALUES ('Miserable/Sad');
INSERT INTO labels (label_name) VALUES ('Andry/Annoyed');
INSERT INTO labels (label_name) VALUES ('Alarmed/Aroused');
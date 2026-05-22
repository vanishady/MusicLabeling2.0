# MusicLabeling WebApp

## Requirements

The following are necessary:

| Tool | Version | Download |
|---|---|---|
| JDK | 11 | https://adoptium.net (Eclipse Temurin 11) |
| MySQL | 8.x | https://dev.mysql.com/downloads/installer/ |
| Apache Tomcat | 10.1.x | https://tomcat.apache.org/download-10.cgi (zip) |

> **Beware:** Tomcat must be **10.1.x** or higher

---

## Step 1 — Setup Environmet Variables

### JAVA_HOME
1. **"Variabili d'ambiente"** → *Modifica le variabili d'ambiente di sistema*
2. Sotto *Variabili di sistema* → **Nuova**:
   - Nome: `JAVA_HOME`
   - Valore: percorso della tua JDK, es. `C:\Program Files\Eclipse Adoptium\jdk-11.0.x`
3. Trova la variabile `Path` → **Modifica** → **Nuovo** → `%JAVA_HOME%\bin`

### AUDIO_STORAGE_PATH
1. Crea una cartella dove verranno salvati i file audio, es. `C:\musiclabeling-audio`
2. Sotto *Variabili di sistema* → **Nuova**:
   - Nome: `AUDIO_STORAGE_PATH`
   - Valore: `C:\musiclabeling-audio`

Clicca OK su tutto e **riapri** qualsiasi terminale aperto.

Verifica:
```powershell
java -version
```

---

## Step 2 — Configura il database

Apri **MySQL Shell** e lancia:
```
\sql
\connect root@localhost
\source C:/percorso/al/progetto/database_setup.sql
```

Questo crea il database `musicLabelling` con tutte le tabelle e due utenti di default:
- `admin` / `admin123` (amministratore)
- `user1` / `password1` (utente normale)

---

## Step 3 — Configura la connessione al database

Vai nella cartella di MusicLabeling
Apri il file `src/main/webapp/WEB-INF/web.xml` e modifica i parametri di connessione in base alla tua installazione MySQL:

```xml
<context-param>
    <param-name>dbUser</param-name>
    <param-value>root</param-value>   <!-- tuo utente MySQL -->
</context-param>
<context-param>
    <param-name>dbPassword</param-name>
    <param-value>LA_TUA_PASSWORD</param-value>
</context-param>
```

> **Nota:** Attenzione, se la password contiene caratteri speciali usa il loro escape

---

## Step 4 — Compila il progetto

Apri PowerShell nella cartella del progetto (dove ci sono le cartelle `src`, `taget` etc.) ed esegui:

```powershell
.\mvnw.cmd package -q
```

Questo genera il file `target\MusicLabeling-1.0-SNAPSHOT.war`.

---

## Step 5 — Deploya su Tomcat

1. Estrai lo ZIP di Tomcat, es. in `C:\tomcat`
2. Copia `target\MusicLabeling-1.0-SNAPSHOT.war` nella cartella `C:\tomcat\webapps\`
3. Avvia Tomcat: doppio click su `C:\tomcat\bin\startup.bat`
4. Attendi qualche secondo, poi apri il browser:

```
http://localhost:8080/MusicLabeling-1.0-SNAPSHOT/
```
> **Nota:** Se vuoi un link pià corto, basta che rinomini il `.war` copiato nella cartella  `C:\tomcat\webapps\`
> Ad esempio: rinomina  `target\MusicLabeling-1.0-SNAPSHOT.war` in `target\MusicLabeling.war` per avere come link: http://localhost:8080/MusicLabeling/

---

## Step 6 — Verifica

Accedi con le credenziali dell'utente `admin`:
- Username: `admin`
- Password: `admin123`

---

## Operazioni di gestione

### Fermare Tomcat
Doppio click su `C:\tomcat\bin\shutdown.bat`

### Aggiungere un nuovo utente

**1. Genera l'hash della password** (dalla cartella del progetto):
```powershell
.\mvnw.cmd "exec:java" "-Dexec.mainClass=it.polimi.mae.musiclabeling.utils.HashPassword" "-Dexec.args=lapassword" -q
```

**2. Inserisci l'utente nel database** (MySQL Shell):
```sql
\sql
\connect root@localhost
USE musicLabelling;
INSERT INTO users (username, password, is_admin) VALUES ('nomeutente', '$2a$10$...hash...', FALSE);
```

### Re-deploy dopo le modifiche al codice
Elimina cartella e .war da `C:\tomcat\webapps\MusicLabeling-1.0-SNAPSHOT\`  
Elimina cartella e .war da `.MusicLabelling2.0\target\MusicLabeling-1.0-SNAPSHOT\`  

```powershell
.\mvnw.cmd package -q
```
Poi: ferma Tomcat → elimina `C:\tomcat\webapps\MusicLabeling-1.0-SNAPSHOT\` → copia il nuovo WAR → riavvia Tomcat.

> **Nota:** Ricorda di fare `CTRL+SHIFT+R` sul browser per refresh forzato.

---

## Risoluzione problemi comuni

| Errore | Causa | Soluzione |
|---|---|---|
| `JAVA_HOME not found` | Variabile non impostata | Vedi Step 1 |
| HTTP 404 al deploy | Errore nel web.xml | Controlla `C:\tomcat\logs\catalina.*.log` |
| `Invalid salt version` | DB con password in chiaro | Le password nel DB devono essere hash BCrypt |
| `Unable to upload file` | `AUDIO_STORAGE_PATH` non impostata | Vedi Step 1, sezione AUDIO_STORAGE_PATH |
| `DB error while uploading` | Schema del database errato | Ricrea il DB da database_setup.sql |

---
## Avviare la webapp

Dopo che si è verificata l'esistenza del file .war in  `C:\tomcat\webapps` (o comunque, nella cartella webapps del percorso di installazione di tomcat):
Vai in `C:\tomcat\bin` e fai doppio click su `startup.bat`
Apri il browser e fai 
```
http://localhost:8080/MusicLabeling-1.0-SNAPSHOT/
```

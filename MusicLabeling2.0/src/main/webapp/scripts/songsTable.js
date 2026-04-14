(function() {
    var pageOrchestrator = new PageOrchestrator();

    /**
     * Defines behaviour on page load event.
     */
    window.addEventListener("load", () => {
        if(sessionStorage.getItem("username") == null) {
            window.location.href = "index.html";
        } else {
            pageOrchestrator.start();
        }
    }, false);

    window.addEventListener('keydown',  (e) => {
        pageOrchestrator.handlePlayerPlayStop(e.key);
    })

    function PersonalMessage(username, messageContainer) {
        this.username = username;
        /*
        Puts the user's name inside the given html element.
         */
        this.show = function() {
            messageContainer.textContent = this.username;
        }
    }

    function SongListHandler(alert, songsContainer, songDetailsTexts, songLabelsHandler, audioPlayer, deleteSongButton) {
        this.songsContainer = songsContainer;
        this.songDetailsParagraphs = songDetailsTexts;
        this.deleteSongButton = deleteSongButton;
        this.audioPlayer = audioPlayer;
        this.playing = false;
        this.alert = alert;
        this.currentSongId = -1;

        this.deleteSongButton.addEventListener('click', () => {
            this.deleteSong(this.currentSongId);
        })

        this.setRowClicked = function(row) {
            const allRows = this.songsContainer.getElementsByTagName('tr')
            for(let i = 1; i < allRows.length; i++) {
                allRows[i].classList.remove('selected')
            }
            row.classList.add('selected')
        }

        this.clickSong = async function (songId) {
            songLabelsHandler.setCurrentSongId(songId)
            this.currentSongId = songId;

            const songInfo = await fetchData(`GetSongInfo?song_id=${encodeURIComponent(songId)}`, this.alert)
            const songLabels = await fetchData(`GetSongLabels?song_id=${encodeURIComponent(songId)}`, this.alert)
            const songWavUrl = await fetchData(`GetSongWav?song_id=${encodeURIComponent(songId)}`, this.alert, true)
            if(songInfo == null || songLabels == null || songWavUrl == null)
                return;

            this.songDetailsParagraphs[0].textContent = songInfo.songName.toUpperCase();
            this.songDetailsParagraphs[1].textContent = songInfo.artist.toUpperCase();

            this.loadAndPlayNewSong(songWavUrl);
        }

        this.show = async function() {
            const songs = await fetchData('GetAllSongs', this.alert);
            if (songs.length === 0) {
                this.alert.textContent = "No songs to load!"
                return
            }
            this.update(songs)
            this.songsContainer.getElementsByTagName('tr')[1].click()
        }

        this.update = function(songsArray) {
            var headerCell, row, songIdCell, songTitleCell, songArtistCell, isLabeledCell;
            this.songsContainer.innerHTML = "";

            const headers = ["ID", "SONG NAME", "ARTIST", "LABELED"]
            const headerRow = document.createElement("tr")
            headerRow.classList.add("header-row")
            headerRow.classList.add("songs-table")
            headerRow.classList.add("transparent-background")
            headers.forEach(function(header) {
                headerCell = document.createElement("th")
                headerCell.classList.add("song-table")
                headerCell.classList.add("transparent-background")
                headerCell.textContent = header
                headerRow.appendChild(headerCell)
            })
            this.songsContainer.appendChild(headerRow)

            var self = this;
            songsArray.forEach(function(song) {
                row = document.createElement("tr");
                row.classList.add("song-table")
                row.classList.add("clickable")
                if (song.hasLabels) {
                    row.classList.add("labeled")
                }

                songIdCell = document.createElement("td");
                songIdCell.classList.add("song-table");
                songIdCell.textContent = song.songId;
                row.appendChild(songIdCell);

                songTitleCell = document.createElement("td");
                songTitleCell.classList.add("song-table");
                songTitleCell.textContent = song.songName;
                row.appendChild(songTitleCell);

                songArtistCell = document.createElement("td");
                songArtistCell.classList.add("song-table");
                songArtistCell.textContent = song.artist;
                row.appendChild(songArtistCell);

                isLabeledCell = document.createElement("td");
                isLabeledCell.classList.add("song-table");
                isLabeledCell.textContent = song.hasLabels ? 'Yes' : 'No';
                row.appendChild(isLabeledCell);

                // IIFE to create a separate scope for each songId
                (function(row, songId) {
                    row.addEventListener("click", () => {
                        self.setRowClicked(row)
                        self.clickSong(songId);
                    }, false);
                })(row, songIdCell.textContent);

                self.songsContainer.appendChild(row);
            });
        }

        this.refresh = async function() {
            const songs = await fetchData('GetAllSongs', this.alert);
            if (songs.length === 0) {
                this.alert.textContent = "No songs to load!"
                return
            }
            this.update(songs)
        }

        this.deleteSong = async function(songId) {
            const response = await postData(`DeleteSong?song_id=${encodeURIComponent(songId)}`, alert)
            pageOrchestrator.refresh();
        }

        this.loadAndPlayNewSong = function(newSongUrl, autoPlay=false) {
            // Determine the MIME type based on the file extension
            let fileType = 'audio/mp3'; // Default type
            if (newSongUrl.endsWith('.wav')) {
                fileType = 'audio/wav';
            } else if (newSongUrl.endsWith('.mp3')) {
                fileType = 'audio/mp3';
            }

            // Reset playback time to zero
            this.audioPlayer.currentTime = 0;
            this.playing = false;

            // Update the source to the new song URL with the determined type
            this.audioPlayer.source = {
                type: 'audio',
                sources: [
                    {
                        src: newSongUrl,
                        type: fileType,
                    },
                ],
            };

            // Optionally, play the new song
            if (autoPlay) {
                this.audioPlayer.play();
            }
        }

        this.onEndEvent = function() {
            this.triggerRestart();
            this.playing = false;
        }

        this.triggerPlayStop = function () {
            if(this.playing) {
                this.audioPlayer.pause()
            }
            else {
                this.audioPlayer.play()
            }
            this.playing = !this.playing;
        }

        this.triggerRestart = function() {
            this.audioPlayer.restart()
        }

        this.fastForward = function(timeAmount) {
            this.audioPlayer.forward(timeAmount)
        }

        this.rewind = function(timeAmount) {
            this.audioPlayer.rewind(timeAmount)
        }

        this.increaseVolume = function(step) {
            this.audioPlayer.increaseVolume(step)
        }

        this.decreaseVolume = function(step) {
            this.audioPlayer.decreaseVolume(step)
        }
    }

    function SongLabelsHandler(alert, songLabelsTable, songLabelSelector, songLabelSubmitButton, audioPlayer) {
        this.alert = alert
        this.songLabelsTable = songLabelsTable
        this.songLabelSelector = songLabelSelector
        this.songLabelSubmitButton = songLabelSubmitButton
        this.audioPlayer = audioPlayer
        this.currentSongId = 1
        this.labels = []

        this.setCurrentSongId = async function(songId) {
            this.currentSongId = songId
            const songLabels = await fetchData(`GetSongLabels?song_id=${encodeURIComponent(this.currentSongId)}`, this.alert)
            this.update(songLabels)
        }

        this.init = async function() {
            this.songLabelSelector.innerHTML = ''
            this.labels = await fetchData("GetAllLabels", this.alert)
            this.labels.forEach((label) => {
                let option = document.createElement('option')
                option.classList.add('song-labels')
                option.value = label.labelId
                option.textContent = label.labelName
                this.songLabelSelector.appendChild(option)
            })
            let self = this
            this.songLabelSubmitButton.addEventListener('click', () => {
                self.uploadNewSongLabel()
            })
        }

        this.update = function(songLabels) {
            let self = this;
            this.songLabelsTable.innerHTML = ''
            const headRow = document.createElement('tr')
            headRow.classList.add('song-labels')
            if (songLabels.length === 0) {
                let headCell  = document.createElement('th')
                headCell.classList.add('song-labels')
                headCell.classList.add('no-labels')
                headCell.textContent = 'This song has no labels yet'.toUpperCase();
                headRow.appendChild(headCell)
                this.songLabelsTable.appendChild(headRow)
                return
            }
            const headers = ['Label', 'Time of Start', 'Time of End', 'Delete Label']
            headers.forEach(function (header) {
                let headCell = document.createElement('th')
                headCell.classList.add('song-labels')
                headCell.textContent = header
                headRow.appendChild(headCell)
            })
            this.songLabelsTable.appendChild(headRow)

            for (let i = 0; i < songLabels.length; i++) {
                let row = document.createElement('tr')
                row.classList.add('song-labels')

                let nameCell = document.createElement('td')
                nameCell.classList.add('song-labels')
                nameCell.textContent = songLabels[i].labelName
                row.appendChild(nameCell)

                let startTimingCell = document.createElement('td')
                startTimingCell.classList.add('song-labels')
                startTimingCell.textContent = (songLabels[i].labelTiming / 1000.0).toFixed(2) + 's'
                row.appendChild(startTimingCell)

                let endTimingCell = document.createElement('td')
                endTimingCell.classList.add('song-labels')
                endTimingCell.textContent = i === songLabels.length-1 ? 'end' : (songLabels[i+1].labelTiming / 1000.0).toFixed(2) + 's'
                row.appendChild(endTimingCell)

                let deleteLabelCell = document.createElement('td');
                deleteLabelCell.classList.add('song-labels');
                deleteLabelCell.classList.add('delete-label');
                deleteLabelCell.classList.add('clickable');
                deleteLabelCell.innerHTML = "<b>DELETE</b>";
                row.append(deleteLabelCell);

                (function(userSongLabelId) {
                    deleteLabelCell.addEventListener("click", () => {
                        self.deleteSongLabel(userSongLabelId);
                    }, false);
                })(songLabels[i].userSongLabelId);

                this.songLabelsTable.appendChild(row)
            }
        }

        this.uploadNewSongLabel = async function() {
            const labelId = this.songLabelSelector.value
            const labelTiming =  this.audioPlayer.currentTime
            const response = await postData(`AddLabelToSong?song_id=${encodeURIComponent(this.currentSongId)}&label_id=${encodeURIComponent(labelId)}&label_timing=${encodeURIComponent(labelTiming)}`, this.alert)
            const songLabels = await fetchData(`GetSongLabels?song_id=${encodeURIComponent(this.currentSongId)}`, this.alert)
            this.update(songLabels)
            pageOrchestrator.refresh()
        }

        this.deleteSongLabel = async function(userSongLabelId) {
            const response = await postData(`DeleteSongLabel?user_song_label_id=${encodeURIComponent(userSongLabelId)}`, alert)
            const songLabels = await fetchData(`GetSongLabels?song_id=${encodeURIComponent(this.currentSongId)}`, this.alert)
            this.update(songLabels)
            pageOrchestrator.refresh()
        }
    }

    function PageOrchestrator() {
        var alertContainer = document.getElementById("alert-container");

        this.exportButton = document.getElementById('export-button');
        this.start = function() {
            personalMessage = new PersonalMessage(sessionStorage.getItem('username'), document.getElementById("personal-message"));
            // personalMessage.show();

            const uploadSongLink = document.getElementById('upload-song-link');
            const deleteSongButton = document.getElementById('delete-song-button');
            const exportButton = document.getElementById('export-button');

            const isAdmin = sessionStorage.getItem("admin") === "true";
            if (isAdmin ) {
                exportButton.removeAttribute("hidden");
                uploadSongLink.removeAttribute("hidden");
                deleteSongButton.removeAttribute("hidden");
            }
            else {
                exportButton.setAttribute("hidden", "true");
                uploadSongLink.setAttribute("hidden", "true");
                deleteSongButton.setAttribute("hidden", "true");
            }

            const audioPlayer = new Plyr('#audio1', {
                controls: [
                    'restart',
                    'play',
                    'progress',
                    'current-time',
                    'duration',
                    'mute',
                    'volume'
                ]
            });

            songLabelsHandler = new SongLabelsHandler(
                alertContainer,
                document.getElementById('song-labels-table'),
                document.getElementById('song-labels-selector'),
                document.getElementById('submit-label-button'),
                audioPlayer
            )

            songListHandler = new SongListHandler(
                alertContainer,
                document.getElementById("songs-table"),
                [document.getElementById("song-name"), document.getElementById("song-artist")],
                songLabelsHandler,
                audioPlayer,
                document.getElementById("delete-song-button")
            )

            audioPlayer.on('ended', () => {
                songListHandler.onEndEvent();
            })

            songLabelsHandler.init()
            songListHandler.show()
        }

        //'Export labels' click processing
        this.exportButton = document.getElementById("export-button")
        this.exportLabels = async function() {
            const response = await postData(`ExportLabels`, alert)
            pageOrchestrator.refresh();
        }

        this.exportButton.addEventListener('click', () => {
            this.exportLabels();
        })

        //'Logout' click processing
        this.logoutButton = document.getElementById("logout-button")

        this.logout= async function() {
            sessionStorage.removeItem("admin");
            sessionStorage.removeItem("username");
            window.location.href = "index.html";
        }

        this.logoutButton.addEventListener('click', () => {
            this.logout();
        })

        //Audio Player interaction
        this.handlePlayerPlayStop = function(key) {
            switch (key) {
                case " ":
                    songListHandler.triggerPlayStop();
                    break;
                case "r":
                    songListHandler.triggerRestart();
                    break;
                case "ArrowLeft":
                    songListHandler.rewind(2.0);
                    break;
                case "ArrowRight":
                    songListHandler.fastForward(2.0);
                    break;
                case "ArrowUp":
                    console.log("increased")
                    songListHandler.increaseVolume(.1);
                    break;
                case "ArrowDown":
                    songListHandler.decreaseVolume(.1);
                    break;
                default:
            }
        }

        this.refresh = function() {
            alertContainer.textContent = "";
            songListHandler.refresh()
        }
    }
}())
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

        const SEGMENT_COLORS = {
            'angry/annoyed':     '#e06666',
            'alarmed/aroused':   '#cf0f0f',
            'delighted/excited': '#ffb262',
            'happy/pleased':     '#ffd966',
            'relaxed/serene':    '#3cc4d8',
            'tired/calm':        '#6fa8dc',
            'depressed/bored':   '#93c47d',
            'miserable/sad':     '#0b5394'
        }
        const SEGMENT_COLOR_FALLBACKS = ['#aaaaaa', '#bbbbbb', '#cccccc', '#999999']
        function segmentColor(labelName, idx) {
            const key = labelName.toLowerCase()
            return SEGMENT_COLORS[key] || SEGMENT_COLOR_FALLBACKS[idx % SEGMENT_COLOR_FALLBACKS.length]
        }

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

        this.renderTimeline = function(songLabels) {
            const timeline = document.getElementById('label-timeline')
            timeline.innerHTML = ''

            if (!songLabels || songLabels.length === 0) return

            const duration = this.audioPlayer.duration
            if (!duration || duration <= 0 || isNaN(duration)) return

            const durationMs = duration * 1000

            for (let i = 0; i < songLabels.length; i++) {
                const startMs = songLabels[i].labelTiming
                const endMs = (i < songLabels.length - 1) ? songLabels[i + 1].labelTiming : durationMs

                const leftPct = startMs / durationMs * 100
                const widthPct = (endMs - startMs) / durationMs * 100

                const seg = document.createElement('div')
                seg.classList.add('label-segment')
                seg.style.left = leftPct + '%'
                seg.style.width = widthPct + '%'
                seg.style.backgroundColor = segmentColor(songLabels[i].labelName, i)
                seg.title = songLabels[i].labelName
                timeline.appendChild(seg)

                // Draw handle at the start of each label
                const handle = document.createElement('div')
                handle.classList.add('label-handle')
                handle.style.left = leftPct + '%'
                handle.dataset.labelId = songLabels[i].userSongLabelId
                handle.dataset.minMs = i === 0 ? 0 : songLabels[i - 1].labelTiming + 500
                handle.dataset.maxMs = (i + 1 < songLabels.length)
                    ? songLabels[i + 1].labelTiming - 500
                    : durationMs - 500

                this._attachHandleDrag(handle, timeline, durationMs, i, songLabels)
                timeline.appendChild(handle)
            }
        }

        this._attachHandleDrag = function(handle, timeline, durationMs, labelIdx, songLabels) {
            let self = this
            let dragging = false
            let startX = 0
            let startLeftPct = 0

            handle.addEventListener('pointerdown', (e) => {
                dragging = true
                startX = e.clientX
                startLeftPct = parseFloat(handle.style.left)
                handle.setPointerCapture(e.pointerId)
                e.preventDefault()
            })

            handle.addEventListener('pointermove', (e) => {
                if (!dragging) return
                const rect = timeline.getBoundingClientRect()
                const deltaPct = (e.clientX - startX) / rect.width * 100
                const newLeftPct = Math.max(
                    parseFloat(handle.dataset.minMs) / durationMs * 100,
                    Math.min(
                        parseFloat(handle.dataset.maxMs) / durationMs * 100,
                        startLeftPct + deltaPct
                    )
                )
                handle.style.left = newLeftPct + '%'

                // Update visuals of the two adjacent segments in real time
                const segments = timeline.querySelectorAll('.label-segment')
                const prevSeg = segments[labelIdx - 1]
                const currSeg = segments[labelIdx]
                if (prevSeg) {
                    const prevLeft = parseFloat(prevSeg.style.left)
                    prevSeg.style.width = (newLeftPct - prevLeft) + '%'
                }
                if (currSeg) {
                    const currRight = parseFloat(currSeg.style.left) + parseFloat(currSeg.style.width)
                    currSeg.style.left = newLeftPct + '%'
                    currSeg.style.width = (currRight - newLeftPct) + '%'
                }
            })

            handle.addEventListener('pointerup', async (e) => {
                if (!dragging) return
                dragging = false

                const newTimingMs = Math.round(parseFloat(handle.style.left) / 100 * durationMs)
                await postData(
                    `UpdateLabelTiming?user_song_label_id=${encodeURIComponent(handle.dataset.labelId)}&new_timing_ms=${encodeURIComponent(newTimingMs)}`,
                    self.alert
                )
                const songLabels = await fetchData(`GetSongLabels?song_id=${encodeURIComponent(self.currentSongId)}`, self.alert)
                self.update(songLabels)
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
                this.renderTimeline(songLabels)
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
                nameCell.classList.add('label-name-cell')
                nameCell.textContent = songLabels[i].labelName
                row.appendChild(nameCell)

                let startTimingCell = document.createElement('td')
                startTimingCell.classList.add('song-labels')
                startTimingCell.textContent = (songLabels[i].labelTiming / 1000.0).toFixed(2) + 's'
                row.appendChild(startTimingCell)

                let endTimingCell = document.createElement('td')
                endTimingCell.classList.add('song-labels')
                endTimingCell.textContent = i === songLabels.length - 1 ? 'end' : (songLabels[i + 1].labelTiming / 1000.0).toFixed(2) + 's'
                row.appendChild(endTimingCell)

                let deleteLabelCell = document.createElement('td');
                deleteLabelCell.classList.add('song-labels');
                deleteLabelCell.classList.add('delete-label');
                deleteLabelCell.classList.add('clickable');
                deleteLabelCell.innerHTML = "<b>DELETE</b>";
                row.append(deleteLabelCell);

                this.songLabelsTable.appendChild(row)

                // VA row (hidden by default, toggled on nameCell click)
                let vaRow = document.createElement('tr')
                vaRow.classList.add('va-row')
                vaRow.style.display = 'none'
                let vaCell = document.createElement('td')
                vaCell.colSpan = 4
                vaCell.classList.add('va-cell')

                const initValence = Math.round((songLabels[i].valence || 0.5) * 100)
                const initArousal = Math.round((songLabels[i].arousal || 0.5) * 100)

                vaCell.innerHTML = `
                    <div class="va-slider-container">
                        <label class="va-label">Valence: <span class="va-val-display">${(initValence / 100).toFixed(2)}</span>
                            <input type="range" class="va-slider" min="0" max="100" value="${initValence}" data-type="valence">
                        </label>
                        <label class="va-label">Arousal: <span class="va-aro-display">${(initArousal / 100).toFixed(2)}</span>
                            <input type="range" class="va-slider" min="0" max="100" value="${initArousal}" data-type="arousal">
                        </label>
                    </div>`
                vaRow.appendChild(vaCell)
                this.songLabelsTable.appendChild(vaRow)

                // Wire up events inside IIFE to capture correct scope
                ;(function(row, vaRow, vaCell, labelData) {
                    let vaDebounce = null
                    let currentValence = labelData.valence || 0.5
                    let currentArousal = labelData.arousal || 0.5

                    nameCell.addEventListener('click', () => {
                        vaRow.style.display = vaRow.style.display === 'none' ? '' : 'none'
                    })

                    deleteLabelCell.addEventListener('click', () => {
                        self.deleteSongLabel(labelData.userSongLabelId)
                    }, false)

                    const valenceSlider = vaCell.querySelector('input[data-type="valence"]')
                    const arousalSlider = vaCell.querySelector('input[data-type="arousal"]')
                    const valDisplay = vaCell.querySelector('.va-val-display')
                    const aroDisplay = vaCell.querySelector('.va-aro-display')

                    valenceSlider.addEventListener('input', () => {
                        currentValence = valenceSlider.value / 100
                        valDisplay.textContent = currentValence.toFixed(2)
                        clearTimeout(vaDebounce)
                        vaDebounce = setTimeout(() => {
                            postData(
                                `UpdateLabelValenceArousal?user_song_label_id=${encodeURIComponent(labelData.userSongLabelId)}&valence=${encodeURIComponent(currentValence)}&arousal=${encodeURIComponent(currentArousal)}`,
                                self.alert
                            )
                        }, 500)
                    })

                    arousalSlider.addEventListener('input', () => {
                        currentArousal = arousalSlider.value / 100
                        aroDisplay.textContent = currentArousal.toFixed(2)
                        clearTimeout(vaDebounce)
                        vaDebounce = setTimeout(() => {
                            postData(
                                `UpdateLabelValenceArousal?user_song_label_id=${encodeURIComponent(labelData.userSongLabelId)}&valence=${encodeURIComponent(currentValence)}&arousal=${encodeURIComponent(currentArousal)}`,
                                self.alert
                            )
                        }, 500)
                    })
                })(row, vaRow, vaCell, songLabels[i])
            }

            this.renderTimeline(songLabels)
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

            audioPlayer.on('ready', () => {
                // Re-render timeline once duration is known after loading a new song
                songLabelsHandler.setCurrentSongId(songLabelsHandler.currentSongId)
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
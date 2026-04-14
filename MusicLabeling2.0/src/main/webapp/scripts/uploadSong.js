(function () {
    window.addEventListener('load', async function () {
        let selectObject = document.getElementById("user-selector")
        const users = await fetchData('GetAllUsers', document.getElementById('alert-container'));
        selectObject.innerHTML = ''
        users.forEach((user) => {
            let option = document.createElement('option')
            option.classList.add('song-labels')
            option.value = user.userId
            option.textContent = user.username
            selectObject.appendChild(option)
        })
    })

    document.addEventListener('DOMContentLoaded', function () {
        const form = document.getElementById('upload-song-form');

        form.addEventListener('submit', function (event) {
            event.preventDefault(); // Prevent the default form submission

            const formData = new FormData(form);
            console.log(formData)
            const alertContainer = document.getElementById('alert-container')
            const inputs = form.getElementsByTagName("input");

            fetch('UploadSong', {
                method: 'POST',
                body: formData
            })
                .then(response => {
                    if (response.status === 200) {
                        for (let i = 0; i < inputs.length; i++) {
                            inputs[i].value = '';
                        }
                        return response.text(); // or .text() if the response is not in JSON format
                    } else if (response.status === 403) {
                        window.location.href = response.getResponseHeader("Location");
                        window.sessionStorage.removeItem('username');
                    }
                    return response.text()
                })
                .then(data => {
                    alertContainer.textContent = data;
                })
                .catch(error => {
                    console.error('There has been a problem with your fetch operation:', error);
                });
        });
    });
})()

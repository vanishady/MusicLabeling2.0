function makeCall(method, url, formElement, cback, reset=true, blob=false) {
    var req= new XMLHttpRequest();
    if (blob) {
        req.responseType = 'blob';
    }
    req.onreadystatechange = function() {
        cback(req)
    };
    req.open(method, url);
    if(formElement == null) {
        req.send();
    } else {
        req.send(new FormData(formElement));
    }
    if(formElement !== null && reset === true) {
        formElement.reset();
    }
}

function fetchData(url, alertContainer, blob=false) {
    return fetch(url)
        .then(response => {
            if(response.status === 200) {
                if (blob)
                    return response.blob()
                return response.json()
            }
            else if(response.status === 403) {
                window.location.href = response.getResponseHeader("Location");
                window.sessionStorage.removeItem('username');
            }
            else {
                alertContainer.textContent = response.text();
            }
        })
        .then(data => {
            if (blob) {
                return URL.createObjectURL(data);
            }
            return data;
        })
        .catch(err => {
            console.log('Error fetching data:', err);
        })
}

function postData(url, alertContainer) {
    return fetch(url, {
        method: 'POST', // Specify the method
    })
        .then(response => {
            if(response.status === 403) {
                window.location.href = response.getResponseHeader("Location");
                window.sessionStorage.removeItem('username');
            }
            else if(response.status !== 200){
                alertContainer.textContent = response.text();
            }
            return response
        })
        .catch(error => {
            console.error('Error:', error); // Handle errors
        });
}
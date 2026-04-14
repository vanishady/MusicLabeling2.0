(function() {
    document.getElementById("loginbutton").addEventListener('click', (e) => {
        var form = e.target.closest("form");
        if(form.checkValidity()) {
            makeCall("POST", 'CheckLogin', e.target.closest("form"),
                function(x) {
                    if(x.readyState === XMLHttpRequest.DONE) {
                        switch(x.status) {
                            case 200:
                                const user = JSON.parse(x.responseText);
                                sessionStorage.setItem('username', user.username);
                                sessionStorage.setItem('admin', user.admin);

                                window.location.href = "songs_table.html";
                                break;
                            default:
                                const message = x.responseText;
                                document.getElementById("errormessage").textContent = message;
                                break;
                        }
                    }
                });
        } else {
            form.reportValidity();
        }
    });
})();
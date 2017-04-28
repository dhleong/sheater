
var gapi = {
    auth: {},
    auth2: {},
    client: {
        drive: {
            files: {}
        }
    }
};

gapi.load = function() {};
gapi.auth.authorize = function() {};

/** @return {gapi.AuthInstance} */
gapi.auth2.getAuthInstance = function() {};
gapi.client.init = function() {};
gapi.client.request = function() {};
gapi.client.drive.files.get = function() {};
gapi.client.drive.files.delete = function() {};
gapi.client.drive.files.list = function() {};

/** @constructor */
gapi.AuthInstance = function() {};
gapi.AuthInstance.prototype.isSignedIn = {
    get: function() {},
    listen: function() {},
};

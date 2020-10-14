var auth = {

    getToken() {
        return $.cookie('GMALL-TOKEN')
    },

    setToken(token) {
        return $.cookie('GMALL-TOKEN', token, {domain: 'gmall.com', expires: 7, path: '/'})
    },

    removeToken() {
        return $.cookie('GMALL-TOKEN', '', {domain: 'gmall.com', expires: 7, path: '/'})
    },

    isTokenExist() {
        return $.cookie('GMALL-TOKEN') && $.cookie('GMALL-TOKEN') != ''
    },

    getUserTempId() {
        return $.cookie('userTempId')
    },

    setUserTempId() {
        var s = [];
        var hexDigits = "0123456789abcdef";
        for (var i = 0; i < 36; i++) {
            s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
        }
        s[14] = "4"; // bits 12-15 of the time_hi_and_version field to 0010
        s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1); // bits 6-7 of the clock_seq_hi_and_reserved to 01
        s[8] = s[13] = s[18] = s[23] = "";

        var uuid = s.join("")
        return $.cookie('userTempId', uuid, {domain: 'gmall.com', expires: 365, path: '/'})
    },

    removeUserTempId() {
        return $.cookie('userTempId', '', {domain: 'gmall.com', expires: 7, path: '/'})
    },

    isUserTempIdExist() {
        return $.cookie('userTempId') && $.cookie('userTempId') != ''
    },

    getUserInfo() {
        if ($.cookie('unick')) {
            return $.cookie('unick')
        }
        return null
    },

    setUserInfo(unick) {
        return $.cookie('unick', unick, {domain: 'gmall.com', expires: 7, path: '/'})
    },

    removeUserInfo() {
        return $.cookie('unick', '', {domain: 'gmall.com', expires: 7, path: '/'})
    },

    isUserInfoExist() {
        return $.cookie('unick') && $.cookie('unick') != ''
    },

    isLogin() {
        return this.isTokenExist()
    }
}




(function (window) {
    'use strict';
    var rules = [];

    function knownRole(role) {
        return role === 'family' || role === 'rescuer' || role === 'admin';
    }

    function pathOf(href) {
        if (typeof href !== 'string' || !href.trim()) return null;
        try {
            var url = new URL(href, window.location.origin + '/');
            if (url.origin !== window.location.origin) return null;
            var path = decodeURIComponent(url.pathname).replace(/\/+/g, '/');
            return new URL(path, window.location.origin).pathname.replace(/\/$/, '') || '/';
        } catch (error) {
            return null;
        }
    }

    function configure(data) {
        rules = [];
        function collect(items) {
            (items || []).forEach(function (item) {
                if (item.href && Array.isArray(item.roles)) {
                    rules.push({path: pathOf(item.href), roles: item.roles, requiresApproval: item.requiresApproval, allowPending: item.allowPending});
                }
                if (item.child) collect(item.child);
            });
        }
        collect(data.menuInfo);
        (data.navigationRules || []).forEach(function (rule) {
            rules.push({path: pathOf(rule.href), roles: rule.roles, prefix: rule.prefix === true, requiresApproval: rule.requiresApproval, allowPending: rule.allowPending});
        });
    }

    function isAllowed(user, href) {
        var role = typeof user === 'string' ? user : user && user.userType;
        var approved = user && typeof user === 'object' && user.rescuerStatus === 'available' && user.status !== 'disabled';
        var path = pathOf(href);
        if (!knownRole(role) || path === null) return false;
        var matches = rules.filter(function (rule) {
            return rule.path === path || (rule.prefix && path.indexOf(rule.path + '/') === 0);
        });
        // Other same-origin business pages keep their existing server-side access checks.
        if (user && user.status === 'disabled') return false;
        if (role === 'rescuer' && !approved && !matches.length) return false;
        return matches.every(function (rule) {
            return Array.isArray(rule.roles) && rule.roles.indexOf(role) !== -1 &&
                !(role === 'rescuer' && !approved && (rule.requiresApproval || !rule.allowPending));
        });
    }

    function filterMenu(user, data) {
        var role = typeof user === 'string' ? user : user && user.userType;
        configure(data);
        function filter(items) {
            return (items || []).reduce(function (visible, item) {
                if (!knownRole(role) || (item.roles && item.roles.indexOf(role) === -1)) return visible;
                if (item.href && !isAllowed(user, item.href)) return visible;
                var copy = Object.assign({}, item);
                if (copy.approvedTitle && user && user.rescuerStatus === 'available') copy.title = copy.approvedTitle;
                if (item.child) {
                    copy.child = filter(item.child);
                    if (!copy.child.length) return visible;
                }
                visible.push(copy);
                return visible;
            }, []);
        }
        return Object.assign({}, data, {menuInfo: filter(data.menuInfo), homeInfo: data.roleHomeInfo && data.roleHomeInfo[role] || data.homeInfo});
    }

    window.GuituRoleNavigation = {configure: configure, isAllowed: isAllowed, filterMenu: filterMenu};
})(window);

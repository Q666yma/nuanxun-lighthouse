(function (window) {
  'use strict';

  function text(value) { return value == null ? '' : String(value).trim(); }
  function validEmail(value) { return value.length <= 320 && /^[^\s@,;]+@[^\s@,;]+\.[^\s@,;]+$/.test(value); }

  function bind(form, options) {
    options = options || {};
    var section = form.querySelector('[data-family-contact]');
    if (!section) return null;
    var fields = {};
    ['name', 'relationship', 'phone', 'email'].forEach(function (key) {
      fields[key] = section.querySelector('[data-contact-field="' + key + '"]');
    });
    var dirty = {};
    var emailAvailable = null; // null: the account lookup has not established whether an email exists.
    ['name', 'relationship', 'phone'].forEach(function (key) {
      fields[key].addEventListener('input', function () { dirty[key] = true; });
    });
    form.addEventListener('reset', function () { dirty = {}; });
    function setDefault(key, value, preserveInput) {
      var input = fields[key];
      if (preserveInput && (dirty[key] || text(input.value))) return;
      input.value = text(value);
      input.defaultValue = input.value;
    }
    var status = section.querySelector('[data-contact-status]');
    function setEmail(value) {
      var email = text(value);
      emailAvailable = validEmail(email);
      setDefault('email', emailAvailable ? email : '', false);
      fields.email.placeholder = emailAvailable ? '使用当前账号绑定邮箱' : '暂无有效绑定邮箱，请填写联系电话';
    }
    if (options.existing) {
      ['name', 'relationship', 'phone'].forEach(function (key) { setDefault(key, options.existing[key], false); });
      setEmail(options.existing.email);
      if (status) status.textContent = emailAvailable
        ? '以下为本次求助保存的联系人资料，可在这里修改。'
        : '已保留本次求助的联系人资料；本任务暂无有效绑定邮箱，请填写有效联系电话，以便搜救队员联系。';
    } else {
      if (status) status.textContent = '正在读取账号中的姓名、绑定邮箱和联系电话…';
      fetch('/api/users/me', {credentials: 'same-origin', cache: 'no-store'})
        .then(function (response) {
          if (!response.ok) throw new Error('account');
          return response.json();
        })
        .then(function (response) {
          if (response.success === false || (response.code !== undefined && response.code !== 0)) throw new Error('account');
          var account = response.data || response;
          setDefault('name', account.name, true);
          setDefault('phone', account.contactPhone, true);
          setEmail(account.email);
          if (status) status.textContent = emailAvailable
            ? '已读取当前账号资料，请确认本次联系人并填写与走失人员的关系。'
            : '当前账号暂无有效绑定邮箱，请填写有效联系电话，并确认联系人姓名及与走失人员的关系。';
        })
        .catch(function () {
          if (status) status.textContent = '账号资料暂未自动填入，请手动填写联系人姓名，联系电话如有请填写；系统会在保存时核对绑定邮箱或联系电话。';
        });
    }
    return {
      read: function () {
        return {name: text(fields.name.value), relationship: text(fields.relationship.value), phone: text(fields.phone.value)};
      },
      validate: function () {
        var value = this.read();
        if (!value.name) { fields.name.focus(); return '请填写本次求助的联系人姓名'; }
        if (!value.relationship) { fields.relationship.focus(); return '请填写您与走失人员的关系'; }
        if (emailAvailable === false && !value.phone) {
          fields.phone.focus(); return '暂无有效绑定邮箱，请填写联系电话以便搜救人员联系';
        }
        if (value.phone && (!/^\+?[0-9() -]+$/.test(value.phone) || value.phone.replace(/[^0-9]/g, '').length < 6 || value.phone.replace(/[^0-9]/g, '').length > 20)) {
          fields.phone.focus(); return '请填写有效联系电话，未知可留空并通过绑定邮箱联系';
        }
        return '';
      }
    };
  }

  window.GuituFamilyContact = {bind: bind};
})(window);

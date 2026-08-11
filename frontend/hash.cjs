const bcrypt = require('bcryptjs');
console.log(bcrypt.hashSync('12345678', 10));

(function(window, undefined)
{
	'use strict';

	var AndroidEws =
	{
		getRooms: function(email, password, roomRootEmail, successFunction, errorFunction) {
			setTimeout(function() {
				cordova.exec(successFunction, errorFunction, 'AndroidEws', 'getRooms', 
					[{
						"email": email, 
						"password": password, 
						"roomRootEmail": roomRootEmail
					}]);
				}, 0);
		},
		
		getAppointments: function(email, password, roomEmail, start, end, successFunction, errorFunction) {			
			setTimeout(function() {
				cordova.exec(successFunction, errorFunction, 'AndroidEws', 'getAppointments', 
				[{
					"email": email, 
					"password": password, 
					"roomEmail": roomEmail,
					"start": start,
					"end": end
				}]);
			}, 0);
		},

		cancelAppointment: function(email, password, roomEmail, start, end, id, key, successFunction, errorFunction) {
			setTimeout(function() {
				cordova.exec(successFunction, errorFunction, 'AndroidEws', 'cancelAppointment',
					[{
						"email": email,
						"password": password,
						"roomEmail": roomEmail,
						"start": start,
						"end": end,
						"uniqueId" : id,
						"changeKey" : key
					}]);
			}, 0);
		},
		
		createAppointment: function(email, password, subject, start, duration, location, address, successFunction, errorFunction) {
			setTimeout(function() {	
				cordova.exec(successFunction, errorFunction, 'AndroidEws', 'createAppointment', 
				[{
					"email": email, 
					"password": password, 
					"subject": subject,
					"start": start,
					"duration": duration,
					"location": location,
					"address": address				
				}]);
			}, 0);
		}
	};
	
	cordova.addConstructor(function() 
	{
		window.AndroidEws = AndroidEws;
		return window.AndroidEws;
	});
	
})(window);

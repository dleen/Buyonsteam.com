	$('input.autocomplete').each( function() {
		var $input = $(this);
		var serverUrl = $input.data('url');
		$input.autocomplete({ source:serverUrl, delay: 30 });
	});

	$('.mywell, .alert').height(function () {
		var h = _.max($(this).closest('.row-fluid').find('.mywell, .alert'), function (elem, index, list) {
			return $(elem).height();
		});
		return $(h).height();
	});
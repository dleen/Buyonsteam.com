$(function () {

		var pricelist = [];
		$.each(data, function(i, d) { pricelist.push(d.y); });
		var unqprices = _.uniq(pricelist).length
		var rainbow = new Rainbow();

		var j=1;
		if(names.length == 1 || unqprices == 1) {
			$.each(data, function(i, d) { d.color = "#ceecb2";});
		} else {
			rainbow.setNumberRange(1, unqprices);
			rainbow.setSpectrum('ceecb2', 'ecb5b2');
			for(var i=0; i<names.length; i++) {
				data[i]['color'] = "#" + rainbow.colorAt(j);
				if(i+1 < names.length)
				{
					if(data[i+1]['y'] > data[i]['y']) {
						j++;
					}
				} else {
					if(data[i]['y'] > data[i-1]['y']) {
						data[i]['color'] = "#" + rainbow.colorAt(j);
					}
				}
			}
		}

		var chart;
		$(document).ready(function() {
			chart = new Highcharts.Chart({
				chart: {
					renderTo: 'barchart-cont',
					type: 'bar',
					backgroundColor: null,
					spacingRight: 0,
					spacingLeft: 0,
					marginRight: -20,
					marginTop: 15,
					marginBottom: 15//,
					//height: 28 * (names.length + 3) - 22 + 20
				},
				title: {
					text: null
				},
				xAxis: {
					categories: names,
					title: {
						text: null
					},
					gridLineWidth: 0,
					lineWidth: 0,					
					tickWidth: 0,
					labels: {
						style: {
							fontSize: '14px',
							color: '#3b3b3b',
							fontWeight: 'bold'
						}
					}
				},
				yAxis: {
					labels: {
						enabled: false
					},
					gridLineWidth: 0,
					lineWidth: 0,
					title: {
						text: null,
						align: 'low',
						style: {
							color: '#3b3b3b'
						}
					}
				},
				tooltip: {
					enabled: true,
					formatter: function (d) { return null; },
					borderWidth: 0,
					shadow: false,
					backgroundColor: null
				},
				legend: {enabled: false},
				navigation: {
					buttonOptions: {
						enabled: false
					}
				},
				plotOptions: {
					bar: {
						dataLabels: {
							enabled: true,
							align: 'right',
							style: {
								color: '#272727',
								fontSize: '13px'
							},
							x: -5,
							y: -2,
							formatter: function() { return "$" + this.y }
						},
						groupPadding: 0,
						pointPadding: 0.01,
						shadow: false,
                	//color: 'darkgray',
                	cursor: 'pointer',
                	point: {
                		events: {
                			click: function (d) { location.href = this.options.url; }
                		}
                	},
                	minPointLength: 50,
                	pointWidth: 40
                }
            },
            credits: {
            	enabled: false
            },
            series: [{
            	name: "Price",
            	data: data
            }]
        });
});
})
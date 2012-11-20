function pricechart(linedata) {
var chart; // globally available
$(document).ready(function() {
    
        chart = new Highcharts.Chart({
            chart: {
                renderTo: 'chart-cont',
                type: 'line',
                backgroundColor: null
            },

            xAxis: {
                type: 'datetime',
    dateTimeLabelFormats: { // don't display the dummy year
    day: '%e. %b',
    week: '%e. %b'
}
},

yAxis: {
    title: {
        text: 'Price'
    }
},

plotOptions: {
    line: {
        dataLabels: {
            enabled: true
        }
    }
},
navigation: {
    buttonOptions: {
        enabled: false
    }
},
credits: {
    enabled: false
},

series: linedata
});
});
}

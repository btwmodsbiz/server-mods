(function($){

var hasStarted = false;

var Stats = window.BTWStats = {
	start: function(url) {
		if (!hasStarted && $.type(url) == "string") {
			
			var worldStats = $("#WorldStats");
			var worldStatsHTML = worldStats.find(">td").html();
			worldStats.empty();
			
			for (var i = 0; i < 3; i++) {
				$("<td></td>")
					.html(worldStatsHTML.replace(/_0_/g, "_" + i + "_"))
					.find(".worldnum").text(i+"").end()
					.appendTo(worldStats);
			}
			
			var worldStatDetails = $("#WorldStatDetails");
			var worldStatDetailsHTML = worldStatDetails.html();
			worldStatDetails.empty();
			
			for (var i = 0; i < 3; i++) {
				$("<div></div>")
					.html(worldStatDetailsHTML.replace(/_0_/g, "_" + i + "_"))
					.find(".worldnum").text(i+"").end()
					.appendTo(worldStatDetails);
			}
			
			var tickerNum = 0;
			var tickerChars = [ '/', '-', '\\', '|' ];
			var ticker = $("<div></div>").css({
				position: "fixed",
				right: "4px",
				top: "2px",
				"font-family": "'Courier New', monospace"
			}).appendTo("body");
			
			var populator = new Stats.FieldPopulater({
				retriever: new Stats.Retriever({
					url: url,
					updateStats: function(json) {
						if (json.detailedMeasurements)
							$("#GeneralStats").removeClass("hideDetailed");
						else
							$("#GeneralStats").addClass("hideDetailed");
						
						populator.set(json);
						
					},
					error: function() {
						console.log("Error");
					},
					complete: function() {
						ticker.text(tickerChars[tickerNum % tickerChars.length]); 
						tickerNum++;
					}
				})
			});
			populator.retriever.get();
			hasStarted = true;
		}
	}
};

Stats.FieldPopulater = function(options) {
	if (options) {
		$.extend(this, options);
	}
	
	if (this.retriever == null) {
		this.retriever = new Stats.Retriever();
	}
	
	if (this.formatter == null) {
		this.formatter = new Stats.TypeFormatter();
	}
};

$.extend(Stats.FieldPopulater.prototype, {
	retriever: null,
	formatter: null,
	elementLookup: {},
	chunkStatsSort: "topTickTime",
	
	fields: {
		"ticksPerSecondArray": function(populator, element, value, json) {
			element.text((value.latest / 100).toFixed(3) + " (" + (value.average / 100).toFixed(3) + " avg)");
		},
		"chunkStats": function(populator, element, value, json) {
			var html = "";
			for (var i = 0; i < value.topTickTime.length; i++) {
				html += "<tr><td>" + value[populator.chunkStatsSort][i] + "</td><td>" + populator.formatter.format("average_nano", value.lookup[value[populator.chunkStatsSort][i]].tickTime, json) + "</td><td>" + populator.formatter.format("", value.lookup[value[populator.chunkStatsSort][i]].count, json) + "</td></tr>";
			}
			html += "<tr><td><strong>Total</strong></td><td>" + populator.formatter.format("nano", value.totalTickTime, json) + "</td><td>" + populator.formatter.format("", value.totalCount, json) + "</td></tr>";
			element.html(html);
		},
		"entityStats": function(populator, element, value, json) {
			this.chunkStats(populator, element, value, json);
		},
		"tileEntityStats": function(populator, element, value, json) {
			this.chunkStats(populator, element, value, json);
		},
		"trackedEntityStats": function(populator, element, value, json) {
			this.chunkStats(populator, element, value, json);
		}
	},
	
	set: function(json, base) {
		if (!base)
			base = "Field";
		
		for (var key in json) {
			var fullKey = base + "_" + key;
			var lookup = this.elementLookup[fullKey];
			
			// Find the element if it is not defined in the lookup.
			if ($.type(lookup) == "undefined") {
				
				// Attempt to find the element.
				var newLookup = { element: $("#" + fullKey), type: null };
				if (newLookup.element.size() == 1) {
				
					// Determine the format type for the key.
					var classes = newLookup.element.attr('class');
					if ($.type(classes) != "undefined") {
						classes = classes.split(/\s+/);
						for (var i in classes) {
							if (classes[i].indexOf("type_") == 0 && $.type(this.formatter.types[classes[i].substring("type_".length)])) {
								newLookup.type = classes[i].substring("type_".length);
							}
						}
					}
					
					lookup = this.elementLookup[fullKey] = newLookup;
				}
			}
			
			var hasSubElements = $.type(json[key]) == "object" || $.type(json[key]) == "array";
			if ($.type(lookup) != "undefined") {
				if (this.fields[key]) {
					this.fields[key](this, lookup.element, json[key], json);
				}
				else if (lookup.type == null && hasSubElements) {
					this.set(json[key], fullKey);
				}
				else {
					lookup.element.text(this.formatter.format(lookup.type, json[key], json));
				}
			}
			else if (hasSubElements) {
				this.set(json[key], fullKey);
			}
		}
	},
	
	hasHandler: function(field, type, value) {
		return $.type(this.fields[field]) != "undefined" || $.type(this.formatter.types[type]) != "undefined" || ($.type(value) != "object" && $.type(value) != "array");
	}
});

Stats.TypeFormatter = function(options) {
	
};

$.extend(Stats.TypeFormatter.prototype, {
	types: {
		"average": function(value, json) {
			return value.latest + " (" + value.average + " avg)";
		},
		"average_latest": function(value, json) {
			return value.latest;
		},
		"average_nano": function(value, json) {
			return (value.latest / 1000000).toFixed(3) + " ms (" + (value.average / 1000000).toFixed(3) + " avg)";
		},
		"nano": function(value, json) {
			return (value / 1000000).toFixed(3) + " ms";
		},
		"players": function(value, json) {
			var players = "";
			for (var i = 0; i < value.length; i++) {
				if (players != "") players += ", ";
				players += value[i];
			}
			return players == "" ? "---" : players;
		}
	},
	
	format: function(type, value, json) {
		if ($.type(this.types[type]) != "undefined")
			return this.types[type](value, json);
		else
			return value + "";
	},
	
	supportsType: function(type) {
		return $.type(this.types[type]) != "undefined";
	}
});

Stats.Retriever = function(options) {
	if (options) {
		$.extend(this.options, options);
	}
};

$.extend(Stats.Retriever.prototype, {
	options: {
		enabled: true,
		url: null,
		delay: 1500,
		updateStats: null,
		error: null,
		complete: null
	},
	
	get: function() {
		var self = this;
		
		if (!this.options.enabled) {
			setTimeout(function() {
				self.get();
			}, this.options.delay);
			return;
		}
		
		$.ajax({
			url: this.options.url,
			cache: false,
			dataType: 'json',
			timeout: this.options.delay,
			
			success: function(data, textStatus, jqXHR) {
				if ($.isFunction(self.options.updateStats)) {
					try { self.options.updateStats(data); }
					catch (err) { console.log(err); }
				}
			},
			
			error: function(jqXHR, textStatus, errorThrown) {
				if ($.isFunction(self.options.error)) {
					try { self.options.error(jqXHR, textStatus, errorThrown); }
					catch (err) { }
				}
			},
			
			complete: function(jqXHR, textStatus) {
				if ($.isFunction(self.options.complete)) {
					try { self.options.complete(jqXHR, textStatus); }
					catch (err) { }
				}
				
				setTimeout(function() {
					self.get();
				}, self.options.delay);
			}
		});
	}
	
});

})(jQuery);
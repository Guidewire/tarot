
ServerSentEvents = function(uri, fnMapData, fnDataReceived) {
  this.init(uri, fnMapData, fnDataReceived);
};

$.extend(ServerSentEvents.prototype, {
  DEFAULT_FN_MAPDATA: function(d) { return d; },

  uri: '',
  eventSource: null,
  fnMapData: null,
  listeners: [],

  init: function(uri, fnMapData) {
    this.uri = uri;
    this.listeners = [];
    this.fnMapData = typeof(fnMapData) === 'undefined' ? this.DEFAULT_FN_MAPDATA : fnMapData;
    this.eventSource = new EventSource(uri);
  },

  addDataReceivedListener: function(listener) {
    this.listeners.push(listener);
  },

  notifyDataReceivedListeners: function(data) {
    var i = -1;
    var listener = null;
    var length = this.listeners.length;

    while (++i < length) {
      listener = this.listeners[i];
      listener(data);
    }
  },

  begin: function() {
    var instance = this;
    this.eventSource.addEventListener("open", function(e) {
      //connection open
    });
    this.eventSource.addEventListener("error", function(e) {
      //error
    });
    this.eventSource.addEventListener("message", function(e) {
      instance.notifyDataReceivedListeners(instance.fnMapData(e.data));
    });
  }
});

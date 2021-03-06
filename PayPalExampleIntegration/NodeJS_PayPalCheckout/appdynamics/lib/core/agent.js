'use strict';

var os = require('os');
var fs = require('fs');
var util = require('util');
var path = require('path');
var cluster = require('cluster');
var EventEmitter = require('events').EventEmitter;

var Logger = require('./logger').Logger;
var Timers = require('./timers').Timers;
var System = require('./system').System;
var Proxy = require('./proxy').Proxy;
var Thread = require('./thread').Thread;
var Utils = require('./utils').Utils;
var MetricsManager = require('../metrics/metrics-manager').MetricsManager;
var ProcessState = require('../process/process-state').ProcessState;
var ProcessInfo = require('../process/process-info').ProcessInfo;
var ProcessStats = require('../process/process-stats').ProcessStats;
var DiskStats = require('../process/disk-stats').DiskStats;
var ProxyLauncher = require('../proxy/proxy-launcher').ProxyLauncher;
var ProxyTransport = require('../proxy/proxy-transport').ProxyTransport;
var ConfigManager = require('../transactions/config-manager').ConfigManager;
var TransactionRegistry = require('../transactions/transaction-registry').TransactionRegistry;
var TransactionNaming = require('../transactions/transaction-naming').TransactionNaming;
var TransactionReporter = require('../transactions/transaction-reporter').TransactionReporter;
var TransactionRules = require('../transactions/transaction-rules').TransactionRules;
var Correlation = require('../transactions/correlation').Correlation;
var Profiler = require('../profiler/profiler').Profiler;
var NamedTransactions = require('../profiler/named-transactions').NamedTransactions;
var CustomTransaction = require('../profiler/custom-transaction').CustomTransaction;
var GCStats = require('../v8/gc-stats').GCStats;
var CpuProfiler = require('../v8/cpu-profiler').CpuProfiler;
var agentVersion = require('../../appdynamics_version.json');

function Agent() {
  this.tmpDir = '/tmp/appdynamics';

  if(!fs.existsSync('/tmp')) {
    this.tmpDir = './tmp/appdynamics'; // Heroku case
  }

  this.logsDir = '/var/log/appdynamics';
  if(!fs.existsSync('/var/log')) {
    this.logsDir = './log/appdynamics'; // Heroku case
  }

  this.initialized = false;
  this.version = agentVersion.version;
  this.nextId = Math.round(Math.random() * Math.pow(10, 6));
  this.appdNative = undefined;

  this.performanceIndexMetric = undefined;

  // predefine options
  this.debug = false;
  this.precompiled = undefined;
  this.features = {
    transactionProfiler: true,
    hostMetrics: true,
    redisMetrics: true,
    mongodbMetrics: true
  };

  EventEmitter.call(this);


  // create modules
  this.logger = new Logger(this);
  this.timers = new Timers(this);
  this.system = new System(this);
  this.proxy = new Proxy(this);
  this.thread = new Thread(this);
  this.utils = new Utils(this);
  this.metricsManager = new MetricsManager(this);
  this.processState = new ProcessState(this);
  this.processInfo = new ProcessInfo(this);
  this.processStats = new ProcessStats(this);
  this.diskStats = new DiskStats(this);
  this.proxyLauncher = new ProxyLauncher(this);
  this.proxyTransport = new ProxyTransport(this);
  this.configManager = new ConfigManager(this);
  this.transactionRegistry = new TransactionRegistry(this);
  this.transactionNaming = new TransactionNaming(this);
  this.transactionReporter = new TransactionReporter(this);
  this.transactionRules = new TransactionRules(this);
  this.correlation = new Correlation(this);
  this.profiler = new Profiler(this);
  this.namedTransactions = new NamedTransactions(this);
  this.customTransaction = new CustomTransaction(this);
  this.gcStats = new GCStats(this);
  this.cpuProfiler = new CpuProfiler(this);
};

util.inherits(Agent, EventEmitter);



Agent.prototype.init = function(opts) {
  var self = this;

  if(self.initialized) return;
  self.initialized = true;

  opts || (opts = {});

  self.opts = opts;

  self.debug = opts.debug;
  self.precompiled = opts.precompiled === undefined || opts.precompiled;

  if(self.opts.excludeAgentFromCallGraph === undefined) {
    self.opts.excludeAgentFromCallGraph = true;
  }

  // Temp directory
  if(self.opts.tmpDir) {
    self.tmpDir = self.opts.tmpDir;
  }
  if(!fs.existsSync(self.tmpDir)) {
    fs.mkdirSync(self.tmpDir);
  }

  // Initialize logger first.
  self.logger.init(self.debug);

  // Load native extention
  self.loadNativeExtention();

  // Initialize core modules first.
  self.timers.init();
  self.system.init();
  self.proxy.init();
  self.thread.init();
  self.utils.init();

  // Initialize data sender.
  self.proxyLauncher.init();
  self.proxyLauncher.start();
  self.proxyTransport.init();
  self.configManager.init();
  self.transactionRegistry.init();
  self.transactionNaming.init();
  self.transactionReporter.init();
  self.transactionRules.init();
  self.correlation.init();

  // Metrics aggregator should be initialize before
  // metric senders.
  self.metricsManager.init();
  // Predefine performance index metric.
  self.performanceIndexMetric =
    this.metricsManager.createMetric('Process', 'Performance index', null, 'index');


  // Initialize the rest.
  self.processState.init();
  self.processInfo.init();
  self.processStats.init();
  if(self.features.hostMetrics) self.diskStats.init();
  self.profiler.init();
  self.namedTransactions.init(opts.namedTransactions);
  self.customTransaction.init();
  self.gcStats.init();
  self.cpuProfiler.init();


  // Prepare probes.
  self.loadProbes();

  try {
    self.emit('session');
  }
  catch(err) {
    self.logger.error(err);
  }
};

Agent.prototype.profile = Agent.prototype.init;


Agent.prototype.loadProbes = function() {
  var self = this;

  // Dynamic probes.
  var probeCons = [];
  probeCons.push(require('../probes/cluster-probe').ClusterProbe);
  probeCons.push(require('../probes/http-probe').HttpProbe);
  probeCons.push(require('../probes/memcached-probe').MemcachedProbe);
  probeCons.push(require('../probes/mongodb-probe').MongodbProbe);
  probeCons.push(require('../probes/mysql-probe').MysqlProbe);
  probeCons.push(require('../probes/net-probe').NetProbe);
  probeCons.push(require('../probes/pg-probe').PgProbe);
  probeCons.push(require('../probes/redis-probe').RedisProbe);
  probeCons.push(require('../probes/socket.io-probe').SocketioProbe);

  var packageProbes = {};
  probeCons.forEach(function(probeCon) {
    var probe = new probeCon(self);
    probe.packages.forEach(function(pkg) {
      packageProbes[pkg] = probe;
    });
  });

  // Trying to preattaching probles.
  for(var name in packageProbes) {
    var ret;
    try {
      if(require.main) {
        ret = require.main.require(name);
      }
    }
    catch(err) {
      ret = undefined;
      // ignore exceptions
    }

    if(ret) {
      self.logger.log('found ' + name + ' module');
      packageProbes[name].attach(ret, name);
    }
  }

  // on demand probe attaching
  self.proxy.after(module.__proto__, 'require', function(obj, args, ret) {
    var probe = packageProbes[args[0]];
    if(probe) {
      probe.attach(ret, args[0]);
    }
  });


  // Explicit probes.
  var ProcessProbe = require('../probes/process-probe').ProcessProbe;
  new ProcessProbe(self).attach(process);
  var GlobalProbe = require('../probes/global-probe').GlobalProbe;
  new GlobalProbe(self).attach(global);
};


Agent.prototype.loadNativeExtention = function() {
  var self = this;

  try {
    self.appdNative = require('../../appdynamics');
  }
  catch(err) {
    self.logger.log("Failed loaded native extention.");
  }

  if(!self.appdNative && self.precompiled) {
    // use precompiled extention

    try {
      var extentionPath = '../../bin/' +
        process.platform + '/' +
        process.arch + '/' +
        process.version + '/' +
        'addon.node';

      self.logger.log("Trying to load precompiled extention " + extentionPath + "...");

      self.appdNative = require(extentionPath);

      if(
        !self.appdNative ||
        !self.appdNative.cpuTime ||
        !self.appdNative.cpuTime()
      ) {
        throw new Error("Failed loading precompiled extention.");
      }
      else {
        self.logger.log("Loaded precompiled extention.");
      }
    }
    catch(err) {
      self.appdNative = undefined;

      self.logger.error(err);
      self.logger.message(
        "Compilation or installation of native extention failed and " +
        "precompiled package was not available or had no matching version. " +
        "Please try the latest version of \"appdynamics\". " +
        "Alternatively, you can continue using AppDynamics without the native extention, " +
        "but will be missing several core features."
      );
    }
  }
};



Agent.prototype.getNextId = function() {
  return this.nextId++
};


Agent.prototype.destroy = function() {
  try {
    this.emit('destroy');
  }
  catch(err) {
    this.logger.error(err);
  }

  this.removeAllListeners();
};


Agent.prototype.time = function(scope, label, context) {
  if(!this.initialized) return;

  return this.customTransaction.start(scope, label, context)
};


Agent.prototype.metric = function(scope, name, value, unit, op) {
  if(!this.initialized) return;

  this.metricsManager.addMetric(scope, name, value, unit, op);
};


Agent.prototype.expressErrorHandler = function() {
  return function(err, req, res, next) {
    res.__caughtException__ = err;
    next(err);
  };
};


var AppDynamics = function() {
  var self = this;

  var agent = new Agent();
  ['profile',
    'switchApp',
    'destroy',
    'time',
    'metric',
    'expressErrorHandler'
  ].forEach(function(meth) {
    self[meth] = function() {
      return agent[meth].apply(agent, arguments);
    };
  });

  ['on',
   'addListener',
   'pause',
   'resume'
  ].forEach(function(meth) {
    self[meth] = function() {
      // deprecated
    };
  });
};

exports = module.exports = new AppDynamics();

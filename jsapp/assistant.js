// -*- mode: js; indent-tabs-mode: nil; js-basic-offset: 4 -*-
//
// This file is part of ThingEngine
//
// Copyright 2016 Giovanni Campagna <gcampagn@cs.stanford.edu>
//
// See COPYING for details
"use strict";

const Q = require('q');
const events = require('events');
const util = require('util');

const SempreClient = require('sabrina').Sempre;
const Sabrina = require('sabrina').Sabrina;

const JavaAPI = require('./java_api');

const COMMANDS = ['send', 'sendPicture', 'sendRDL', 'sendChoice', 'sendLink', 'sendButton', 'sendAskSpecial'];
const AssistantJavaApi = JavaAPI.makeJavaAPI('Assistant', [],
    COMMANDS,
    ['onhandlecommand', 'onhandleparsedcommand', 'onhandlepicture']);

var instance_;

class LocalUser {
    constructor() {
        this.id = 0;
        this.account = 'INVALID';
        this.name = platform.getSharedPreferences().get('user-name');
    }
}

class AssistantDispatcher {
    constructor(engine) {
        this.engine = engine;

        this._sempre = new SempreClient();
        this._conversation = new Sabrina(this.engine, new LocalUser(), this, true);

        instance_ = this;
    }

    static get() {
        return instance_;
    }

    start() {
        this._sempre.start();
        this._session = this._sempre.openSession();

        AssistantJavaApi.onhandlecommand = this._onHandleCommand.bind(this);
        AssistantJavaApi.onhandleparsedcommand = this._onHandleParsedCommand.bind(this);
        AssistantJavaApi.onhandlepicture = this._onHandlePicture.bind(this);

        this._conversation.start();
    }

    stop() {
        this._sempre.stop();

        AssistantJavaApi.onhandlecommand = null;
        AssistantJavaApi.onhandleparsedcommand = null;
        AssistantJavaApi.onhandlepicture = null;
    }

    getConversation() {
        return this._conversation;
    }

    _onHandleParsedCommand(error, json) {
        return this._conversation.handleCommand(null, json).catch(function(e) {
            console.log('Failed to handle assistant command: ' + e.message);
        });
    }

    _onHandleCommand(error, text) {
        return this._session.sendUtterance(text).then(function(analyzed) {
            return this._conversation.handleCommand(text, analyzed);
        }.bind(this)).catch(function(e) {
            console.log('Failed to handle assistant command: ' + e.message);
        });
    }

    _onHandlePicture(error, url) {
        return this._conversation.handlePicture(url).catch(function(e) {
            console.log('Failed to handle assistant picture: ' + e.message);
        });
    }
};
COMMANDS.forEach(function(c) {
    AssistantDispatcher.prototype[c] = AssistantJavaApi[c];
});

module.exports = AssistantDispatcher;
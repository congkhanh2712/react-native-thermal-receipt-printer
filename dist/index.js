var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function (t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
import { NativeModules, NativeEventEmitter, Platform } from "react-native";
import * as EPToolkit from "./utils/EPToolkit";
import BufferHelper from "./utils/buffer-helper";
import { Buffer } from 'buffer';
var RNUSBPrinter = NativeModules.RNUSBPrinter;
var RNBLEPrinter = NativeModules.RNBLEPrinter;
var RNNetPrinter = NativeModules.RNNetPrinter;
var textTo64Buffer = function (text, opts) {
    var defaultOptions = {
        beep: false,
        cut: false,
        tailingLine: false,
        encoding: "UTF8",
    };
    var options = __assign(__assign({}, defaultOptions), opts);
    var buffer = EPToolkit.exchange_text(text, options);
    return buffer.toString("base64");
};
var bytesToString = function (data, type) {
    var bytes = new BufferHelper();
    bytes.concat(Buffer.from(data));
    var buffer = bytes.toBuffer();
    // console.log('string to print : ', buffer.toString(type))

    return buffer.toString(type);
};
var billTo64Buffer = function (text, opts) {
    var defaultOptions = {
        beep: true,
        cut: true,
        encoding: "UTF8",
        tailingLine: true,
    };
    var options = __assign(__assign({}, defaultOptions), opts);
    var buffer = EPToolkit.exchange_text(text, options);
    return buffer.toString("base64");
};
var textPreprocessingIOS = function (text) {
    var options = {
        beep: true,
        cut: true,
    };
    return {
        text: text
            .replace(/<\/?CB>/g, "")
            .replace(/<\/?C>/g, "")
            .replace(/<\/?B>/g, ""),
        opts: options,
    };
};
// const imageToBuffer = async (imagePath: string, threshold: number = 60) => {
//   const buffer = await EPToolkit.exchange_image(imagePath, threshold);
//   return buffer.toString("base64");
// };
export var USBPrinter = {
    init: function () {
        return new Promise(function (resolve, reject) {
            return RNUSBPrinter.init(function () { return resolve(); }, function (error) { return reject(error); });
        });
    },
    getDeviceList: function () {
        return new Promise(function (resolve, reject) {
            return RNUSBPrinter.getDeviceList(function (printers) { return resolve(printers); }, function (error) { return reject(error); });
        });
    },
    connectPrinter: function (vendorId, productId) {
        return new Promise(function (resolve, reject) {
            return RNUSBPrinter.connectPrinter(vendorId, productId, function (printer) { return resolve(printer); }, function (error) { return reject(error); });
        });
    },
    closeConn: function () {
        return new Promise(function (resolve) {
            RNUSBPrinter.closeConn();
            resolve();
        });
    },
    printText: function (text, opts) {
        if (opts === void 0) { opts = {}; }
        return RNUSBPrinter.printRawData(textTo64Buffer(text, opts), function (error) {
            return console.warn(error);
        });
    },
    printBill: function (text, opts) {
        if (opts === void 0) { opts = {}; }
        return RNUSBPrinter.printRawData(billTo64Buffer(text, opts), function (error) {
            return console.warn(error);
        });
    },
};
export var BLEPrinter = {
    init: function () {
        return new Promise(function (resolve, reject) {
            return RNBLEPrinter.init(function () { return resolve(); }, function (error) { return reject(error); });
        });
    },
    getDeviceList: function () {
        return new Promise(function (resolve, reject) {
            return RNBLEPrinter.getDeviceList(function (printers) { return resolve(printers); }, function (error) { return reject(error); });
        });
    },
    connectPrinter: function (inner_mac_address) {
        return new Promise(function (resolve, reject) {
            return RNBLEPrinter.connectPrinter(inner_mac_address, function (printer) { return resolve(printer); }, function (error) { return reject(error); });
        });
    },
    closeConn: function () {
        return new Promise(function (resolve) {
            RNBLEPrinter.closeConn();
            resolve();
        });
    },
    printText: function (text, opts) {
        if (opts === void 0) { opts = {}; }
        if (Platform.OS === "ios") {
            var processedText = textPreprocessingIOS(text);
            RNBLEPrinter.printRawData(processedText.text, processedText.opts, function (error) { return console.warn(error); });
        }
        else {
            RNBLEPrinter.printRawData(textTo64Buffer(text, opts), function (error) {
                return console.warn(error);
            });
        }
    },
    printBill: function (text, opts) {
        if (opts === void 0) { opts = {}; }
        if (Platform.OS === "ios") {
            var processedText = textPreprocessingIOS(text);
            RNBLEPrinter.printRawData(processedText.text, processedText.opts, function (error) { return console.warn(error); });
        }
        else {
            RNBLEPrinter.printRawData(billTo64Buffer(text, opts), function (error) {
                return console.warn(error);
            });
        }
    },
};
export var NetPrinter = {
    init: function () {
        return new Promise(function (resolve, reject) {
            return RNNetPrinter.init(function () { return resolve(); }, function (error) { return reject(error); });
        });
    },
    getDeviceList: function () {
        return new Promise(function (resolve, reject) {
            return RNNetPrinter.getDeviceList(function (printers) { return resolve(printers); }, function (error) { return reject(error); });
        });
    },
    connectPrinter: function (host, port) {
        return new Promise(function (resolve, reject) {
            return RNNetPrinter.connectPrinter(host, port, function (printer) { return resolve(printer); }, function (error) { return reject(error); });
        });
    },
    closeConn: function () {
        return new Promise(function (resolve) {
            RNNetPrinter.closeConn();
            resolve();
        });
    },
    printText: function (text, opts) {
        if (opts === void 0) { opts = {}; }
        if (Platform.OS === "ios") {
            var processedText = textPreprocessingIOS(text);
            RNNetPrinter.printRawData(processedText.text, processedText.opts, function (error) { return console.warn(error); });
        }
        else {
            RNNetPrinter.printRawData(textTo64Buffer(text, opts), function (error) {
                return console.warn(error);
            });
        }
    },
    printBill: function (text, opts) {
        if (opts === void 0) { opts = {}; }
        if (Platform.OS === "ios") {
            var processedText = textPreprocessingIOS(text);
            RNNetPrinter.printRawData(processedText.text, processedText.opts, function (error) { return console.warn(error); });
        }
        else {
            RNNetPrinter.printRawData(billTo64Buffer(text, opts), function (error) {
                return console.warn(error);
            });
        }
    },
    printRawData: function (data, onError) {
        if (onError === void 0) { onError = function () { }; }
        if (Platform.OS === "ios") {
            var processedText = bytesToString(data, 'hex');
            RNNetPrinter.printHex(processedText, { beep: false, cut: false }, function (error) {
                if (onError) {
                    onError(error);
                }
            });
        }
        else {
            console.log('data in printRawData: ', data)
            RNNetPrinter.printRawData(bytesToString(data, 'base64'), function (error) {
                if (onError) {
                    onError(error);
                }
            });
        }
    },
    printImage: function (data, width, cutPaper, openCashDrawer, onError) {
        if (onError === void 0) { onError = function () { }; }
        if (Platform.OS === "ios") {
            // console.log('data in printImage: ', data, width)
            // RNNetPrinter.printImage(data, { width, cutPaper }, function (error) {
            //     if (onError) {
            //         onError(error);
            //     }
            // });
            let kick = false;
            if(openCashDrawer != ""){
                kick = true;
            }
            return new Promise(function (resolve, reject) {
                return RNNetPrinter.printImage(data, { width, cutPaper, openCashDrawer: kick, }, function (success) { return resolve(success); },function (error) { return reject(error); });
            });
        }
        else {
            // console.log('data in printImage: ', data, width)
            // RNNetPrinter.printImage(data, width, cutPaper, function (error) {
            //     if (onError) {
            //         onError(error);
            //     }
            // });
            let kick = false;
            if(openCashDrawer != ""){
                kick = true;
            }
            return new Promise(function (resolve, reject) {
                return RNNetPrinter.printImage(data, width, cutPaper, kick, function (success) { return resolve(success); },function (error) { return reject(error); });
            });
        }
    }
};
export var NetPrinterEventEmitter = new NativeEventEmitter(RNNetPrinter);
export var RN_THERMAL_RECEIPT_PRINTER_EVENTS;
(function (RN_THERMAL_RECEIPT_PRINTER_EVENTS) {
    RN_THERMAL_RECEIPT_PRINTER_EVENTS["EVENT_NET_PRINTER_SCANNED_SUCCESS"] = "scannerResolved";
    RN_THERMAL_RECEIPT_PRINTER_EVENTS["EVENT_NET_PRINTER_SCANNED_ERROR"] = "registerError";
})(RN_THERMAL_RECEIPT_PRINTER_EVENTS || (RN_THERMAL_RECEIPT_PRINTER_EVENTS = {}));

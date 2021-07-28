package org.mpashka.findme.miband;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class MiBand {

    public static final int GATT_READ_NOT_PERMIT = 2;
    public static final int GATT_WRITE_NOT_PERMIT = 3;


    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb"; //this is common for all BTLE devices. see http://stackoverflow.com/questions/18699251/finding-out-android-bluetooth-le-gatt-profiles

    /**
     * Heart rate service
     */
    private static final UUID UUID_NOTIFICATION_HEARTRATE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    /**
     * Used for reading heart rate data
     */
    private static final UUID UUID_CHAR_HEARTRATE = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_CHARACTERISTIC_AUTH = UUID.fromString("00000009-0000-3512-2118-0009af100700");

    /**
     * Used for enabling/disabling vibration
     */
    public static final UUID UUID_CHARACTERISTIC_7_REALTIME_STEPS = UUID.fromString("00000007-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_ALERT_LEVEL = UUID.fromString((String.format(BASE_UUID, "2A06")));

    public static final UUID UUID_CHARACTERISTIC_3_CONFIGURATION = UUID.fromString("00000003-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_6_BATTERY_INFO = UUID.fromString("00000006-0000-3512-2118-0009af100700");

    public static final UUID UUID_CHARACTERISTIC_DEVICEEVENT = UUID.fromString("00000010-0000-3512-2118-0009af100700");

    /**
     * In some logs it's 0x0...
     */
    public static final byte AUTH_BYTE = 0x08;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 1: sending a "secret" key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_KEY = 0x01;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 2: requesting a random authentication key from the band.
     * This is byte 0, followed by {@link #AUTH_BYTE}.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_REQUEST_RANDOM_AUTH_NUMBER = 0x02;
    /**
     * Received in response to any authentication requests (byte 0 in the byte[] value.
     */
    public static final byte AUTH_RESPONSE = 0x10;
    /**
     * Received in response to any authentication requests (byte 2 in the byte[] value.
     * 0x01 means success.
     */
    public static final byte AUTH_SUCCESS = 0x01;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 3: sending the encrypted random authentication key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the encrypted random authentication key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_ENCRYPTED_AUTH_NUMBER = 0x03;

    private static final byte[] START_HEART_RATE_SCAN = {21, 2, 1};

    public static final UUID UUID_CHARACTERISTIC_CHUNKEDTRANSFER = UUID.fromString("00000020-0000-3512-2118-0009af100700");

    public static final byte FELL_ASLEEP = 0x01;
    public static final byte WOKE_UP = 0x02;
    public static final byte STEPSGOAL_REACHED = 0x03;
    public static final byte BUTTON_PRESSED = 0x04;
    public static final byte START_NONWEAR = 0x06;
    public static final byte CALL_REJECT = 0x07;
    public static final byte FIND_PHONE_START = 0x08;
    public static final byte CALL_IGNORE = 0x09;
    public static final byte ALARM_TOGGLED = 0x0a;
    public static final byte BUTTON_PRESSED_LONG = 0x0b;
    public static final byte TICK_30MIN = 0x0e; // unsure
    public static final byte FIND_PHONE_STOP = 0x0f;
    public static final byte MTU_REQUEST = 0x16;
    public static final byte MUSIC_CONTROL = (byte) 0xfe;


    private static final byte authFlags = 0x00;  // From Mi Band 3
    private static final byte cryptFlags = (byte) 0x80; // From Mi Band 4


    private RxBleDevice device;
    private RxBleConnection connection;
    private Disposable stateDisposable;
    private CompositeDisposable compositeDisposable;
//    private PublishSubject<Boolean> authObservable = PublishSubject.create();

    public void init(RxBleClient bleClient, String address) {
        device = bleClient.getBleDevice(address);
        Timber.i("Name: %s", device.getName());

        stateDisposable = device.observeConnectionStateChanges()
                .subscribe(s -> Timber.i("State changed: %s", s));
    }

    public Observable<RxBleConnection> connect() {
        compositeDisposable = new CompositeDisposable();

        return device.establishConnection(false)
                .doOnSubscribe(d -> stateDisposable = d)
                .doOnNext(c -> {
                    Timber.i("Connection established");
                    connection = c;
                })
                .doFinally(() -> {
                    Timber.i("Connection finally called");
                })

                .flatMap(i -> setupNotification(UUID_CHARACTERISTIC_DEVICEEVENT, o -> compositeDisposable.add(o.subscribe(this::handleDeviceEvent))))
//                .flatMap(i -> configurationSubscribe())
//                .flatMap(i -> auth())
                .map(i -> connection);
//                .subscribe();
//        stateDisposable.dispose();
    }

    public void disconnect() {
        close();
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    public void close() {
        if (stateDisposable != null) {
            stateDisposable.dispose();
        }
    }

    private Single<byte[]> read(UUID characteristicUuid) {
        Timber.i("Reading [%s]...", characteristicUuid);
        return connection.readCharacteristic(characteristicUuid)
                .onErrorResumeNext(e -> {
                    if (e instanceof BleGattCharacteristicException && ((BleGattCharacteristicException) e).getStatus() == GATT_READ_NOT_PERMIT) {
                        Timber.i("Write error. Auth required");
                        return auth()
                                .firstOrError()
                                .flatMap(i -> connection.readCharacteristic(characteristicUuid));
                    }
                    return Single.error(e);
                })
                .doOnSuccess(bytes -> Timber.i("Read [%s] bytes: %s", characteristicUuid, bytesToHex(bytes)));
    }

    private Single<byte[]> write(UUID characteristicUuid, byte[] bytes) {
        Timber.i("Writing [%s]: %s...", characteristicUuid, bytesToHex(bytes));
        return connection.writeCharacteristic(characteristicUuid, bytes)
                .onErrorResumeNext(e -> {
                    if (characteristicUuid != UUID_CHARACTERISTIC_AUTH && e instanceof BleGattCharacteristicException
                            && ((BleGattCharacteristicException) e).getStatus() == GATT_WRITE_NOT_PERMIT) {
                        Timber.i("Write error. Auth required");
                        return auth()
                                .firstOrError()
                                .flatMap(i -> connection.writeCharacteristic(characteristicUuid, bytes));
                    }
                    return Single.error(e);
                })
                .doOnSuccess(data -> Timber.i("Write [%s] bytes result: %s", characteristicUuid, bytesToHex(bytes)));
    }

    public Observable<Observable<byte[]>> setupNotification(UUID characteristicUuid, Consumer<Observable<byte[]>> notificationObserver) {
        return setupNotification(characteristicUuid, i -> i, notificationObserver);
    }

    public <T> Observable<Observable<byte[]>> setupNotification(UUID characteristicUuid, ObservableTransformer<byte[], T> transformer, Consumer<Observable<T>> notificationObserver) {
        return connection.setupNotification(characteristicUuid)
                .doOnNext(observable -> Timber.i("Notifications subscribe for [%s]", characteristicUuid))
                .doOnNext(observable -> notificationObserver.accept(observable
                        .doOnNext(bytes -> Timber.i("Notify [%s] bytes: %s", characteristicUuid, bytesToHex(bytes)))
                        .compose(transformer)
                        .doOnNext(data -> Timber.i("Notify [%s] object: %s", characteristicUuid, data))
                ));
    }

    public <T> Observable<T> setupNotificationFlow(UUID characteristicUuid,
                                                   Function<Observable<byte[]>, ? extends SingleSource<?>> action,
                                                   ObservableTransformer<byte[], T> transformer)
    {
        Observable<Observable<byte[]>> notificationObservable = connection.setupNotification(characteristicUuid);
        return notificationObservable
                .doOnNext(o -> Timber.i("Notifications subscribe for [%s]", characteristicUuid))
                .flatMapSingle(action)
                .flatMap(i -> notificationObservable)
                .flatMap(observable -> observable
                        .doOnNext(bytes -> Timber.i("Notify [%s] bytes: %s", characteristicUuid, bytesToHex(bytes)))
                )
                .compose(transformer)
                .doOnNext(data -> Timber.i("Notify [%s] object: %s", characteristicUuid, data));
    }

    public Observable<Boolean> auth() {
        Timber.i("Start auth");
        return setupNotificationFlow(UUID_CHARACTERISTIC_AUTH,
                o -> sendAuth(),
                o -> o)
                .flatMapMaybe(this::handleAuth)
                .doOnNext(a -> Timber.i("Authenticated %s", a));
    }

    public Single<byte[]> sendAuth() {
/* needAuth - true
        byte[] sendKey = new byte[2 + 16];
        sendKey[0] = AUTH_SEND_KEY;
        sendKey[1] = authFlags;
        System.arraycopy(getSecretKey(), 0, sendKey, 2, 16);
        return write(UUID_CHARACTERISTIC_AUTH, sendKey);
*/

//      needAuth - false
        return write(UUID_CHARACTERISTIC_AUTH, requestAuthNumber());
    }

    public Maybe<Boolean> handleAuth(byte[] value) throws NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException {
        Timber.i("Received characteristic auth");
        if (value[0] != AUTH_RESPONSE) {
            Timber.i("Unknown auth request");
            return Maybe.empty();
        }

        if (value[1] == AUTH_SEND_KEY && value[2] == AUTH_SUCCESS) {
            Timber.i("Sending the secret key to the device");
            return write(UUID_CHARACTERISTIC_AUTH, requestAuthNumber())
                    .flatMapMaybe(i -> Maybe.empty());
        } else if ((value[1] & 0x0f) == AUTH_REQUEST_RANDOM_AUTH_NUMBER && value[2] == AUTH_SUCCESS) {
            byte[] eValue = handleAESAuth(value, getSecretKey());
            byte[] responseValue = new byte[eValue.length + 2];
            responseValue[0] = (byte) (AUTH_SEND_ENCRYPTED_AUTH_NUMBER | cryptFlags);
            responseValue[1] = authFlags;
            System.arraycopy(eValue, 0, responseValue, 2, eValue.length);
            Timber.i("Sending the encrypted random key to the device");
            return write(UUID_CHARACTERISTIC_AUTH, responseValue)
                    .flatMapMaybe(i -> Maybe.empty());
//                        huamiSupport.setCurrentTimeWithService(builder);
        } else if ((value[1] & 0x0f) == AUTH_SEND_ENCRYPTED_AUTH_NUMBER && value[2] == AUTH_SUCCESS) {
            Timber.i("Authenticated");
//            authObservable.onNext(true);
            return Maybe.just(true);
        } else {
            Timber.i("Unknown auth response request");
        }
        return Maybe.empty();
    }

    private byte[] requestAuthNumber() {
        if (cryptFlags == 0x00) {
            return new byte[]{AUTH_REQUEST_RANDOM_AUTH_NUMBER, authFlags};
        } else {
            return new byte[]{(byte) (cryptFlags | AUTH_REQUEST_RANDOM_AUTH_NUMBER), authFlags, 0x02, 0x01, 0x00};
        }
    }

    // todo
    private byte[] getSecretKey() {
//        byte[] authKeyBytes = new byte[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45};
//        0x62e87546956e6c8b24b45abdb0a63a08
        byte[] authKeyBytes = new byte[]{0x62, (byte) 0xe8, 0x75, 0x46, (byte) 0x95, 0x6e, 0x6c, (byte) 0x8b, 0x24, (byte) 0xb4, 0x5a, (byte) 0xbd, (byte) 0xb0, (byte) 0xa6, 0x3a, 0x08};
        assert authKeyBytes.length == 16;
        return authKeyBytes;
    }

    private byte[] handleAESAuth(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        byte[] mValue = Arrays.copyOfRange(value, 3, 19);
        @SuppressLint("GetInstance") Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.ENCRYPT_MODE, newKey);
        return ecipher.doFinal(mValue);
    }


    public Single<Integer> readBatteryInfo() {
        Timber.i("Reading Battery Info");
        return read(UUID_CHARACTERISTIC_6_BATTERY_INFO)
                .map(value -> {
                    int level = value.length >=2 ? value[1] : 50;
                    Timber.i("Charge level: %s", level);
                    if (value.length >= 3) {
                        switch (value[2]) {
                            case 0:
                                Timber.i("Charge normal");
                                break;
                            case 1:
                                Timber.i("Charging");
                                break;
                            default:
                                Timber.i("Charging unknown: %s", value[2]);
                                break;
                        }
                    }
                    if (value.length >= 18) {
                        Calendar lastCharge = rawBytesToCalendar(value, 11);
                        Timber.i("Last charge: %s", lastCharge);
                    }
                    return level;
                });
    }

    /**
     * uses the standard algorithm to convert bytes received from the MiBand to a Calendar object
     *
     * @param value
     * @return
     */
    public static GregorianCalendar rawBytesToCalendar(byte[] value, int offset) {
        if (value.length >= 7) {
            GregorianCalendar timestamp = new GregorianCalendar(
                    toUint16(value, offset),
                    (value[offset+2] & 0xff) - 1,
                    value[offset+3] & 0xff,
                    value[offset+4] & 0xff,
                    value[offset+5] & 0xff,
                    value[offset+6] & 0xff
            );

            if (value.length > 7+offset) {
                TimeZone timeZone = TimeZone.getDefault();
                timeZone.setRawOffset(value[offset+7] * 15 * 60 * 1000);
                timestamp.setTimeZone(timeZone);
            }
            return timestamp;
        }

        return new GregorianCalendar();
    }

    public static int toUint16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

/*
    private HuamiSupport requestAlarms(TransactionBuilder builder) {
        LOG.info("Requesting alarms");
        builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_3_CONFIGURATION), HuamiService.COMMAND_REQUEST_ALARMS);
        return this;
    }
*/

    public Observable<Observable<byte[]>> configurationSubscribe() {
        return setupNotification(UUID_CHARACTERISTIC_3_CONFIGURATION, o -> o
                .subscribe(value -> {
                    if (value == null || value.length < 4) {
                        Timber.i("Unknown characteristic %s", bytesToHex(value));
                        return;
                    }
                    if (value[0] == 0x10 && value[2] == 0x01) {
                        if (value[1] == 0x0e) {
                            String gpsVersion = new String(value, 3, value.length - 3);
                            Timber.i("got gps version = %s", gpsVersion);
                        } else if (value[1] == 0x0d) {
                            Timber.i("got alarms from watch");
                            decodeAndUpdateAlarmStatus(value);
                        } else {
                            Timber.i("got configuration info we do not handle yet %s", bytesToHex(value));
                        }
                    } else {
                        Timber.i("error received from configuration request %s", bytesToHex(value));
                    }
                })
        );
    }

    private void decodeAndUpdateAlarmStatus(byte[] response) {
        int maxAlarms = 10;
        int nr_alarms = response[8];
        for (int i = 0; i < nr_alarms; i++) {
            byte alarm_data = response[9 + i];
            int index = alarm_data & 0xf;
            if (index >= maxAlarms) {
                Timber.i("Unexpected alarm index from device, ignoring: %s", index);
                return;
            }
            boolean enabled = (alarm_data & 0x10) == 0x10;
            Timber.i("alarm %s is enabled: %s", index, enabled);
        }
    }

    /**
     * Starts heart rate scanner
     */
//    public Observable<Observable<byte[]>> heartRateSubscribe(Consumer<Observable<Integer>> rateNotificationObserver) {
//        return setupNotification(UUID_NOTIFICATION_HEARTRATE, s -> s.map(data -> {
    public Observable<Integer> heartRateSubscribe() {
        return setupNotificationFlow(UUID_NOTIFICATION_HEARTRATE,
                o -> heartRateScan(),
                o -> o.map(data -> {
                    if (data.length == 2 && data[0] == 0) {
                        // 6 for mi band hz
                        // 0 for mi band 2
                        int heartRate = data[1] & 0xFF;
                        Timber.i("Heart rate %s", heartRate);
                        return heartRate;
//                        observer.onNext(heartRate);
//                        listener.onNotify(heartRate);
                    } else {
                        throw new RuntimeException("Unknown data");
                    }
                }));
    }

    public Single<byte[]> heartRateScan() {
        return write(UUID_CHAR_HEARTRATE, START_HEART_RATE_SCAN)
                .doOnSuccess(data -> Timber.i("Start scan success %s", Arrays.equals(data, START_HEART_RATE_SCAN)));
    }

    /**
     * @param msg
     * @param customIconId 0..36
     * @return
     */
    public Single<byte[]> sendNotification(String msg, String app, byte customIconId) {
        String appName = "\0" + app + "\0";
        byte[] appSuffix = appName.getBytes();
        int suffixlength = appSuffix.length;
        boolean hasExtraHeader = true;  // MiBand4
        int prefixlength = 3 + 4; // 4 - extraHeader
        byte[] rawmessage = msg.getBytes();
        int length = msg.length();
        byte[] command = new byte[length + prefixlength + suffixlength];
        int pos = 0;

        byte alertCategory = (byte) -6;

        command[pos++] = alertCategory;
        if (hasExtraHeader) {
            command[pos++] = 0; // TODO
            command[pos++] = 0;
            command[pos++] = 0;
            command[pos++] = 0;
        }
        command[pos++] = 1;
        command[pos] = customIconId;
        System.arraycopy(rawmessage, 0, command, prefixlength, length);
        System.arraycopy(appSuffix, 0, command, prefixlength + length, appSuffix.length);
        return writeChunked(0, command);
    }

    public Single<Integer> readSteps() {
        return read(UUID_CHARACTERISTIC_7_REALTIME_STEPS)
                .map(value -> {
                    int steps = 0;
                    if (value.length == 13) {
//                        byte[] stepsValue = new byte[] {value[1], value[2]};
                        steps = (value[1] & 0xff) | ((value[2] & 0xff) << 8);
                        Timber.i("realtime steps: %s", steps);
                    } else {
                        Timber.i("Unrecognized realtime steps value: %s", bytesToHex(value));
                    }
                    return steps;
                });
    }

    private Single<byte[]> writeChunked(int type, byte[] data) {
        Single<byte[]> lastChunk = null;
        final int MAX_CHUNKLENGTH = connection.getMtu() - 6;
        int remaining = data.length;
        byte count = 0;
        while (remaining > 0) {
            int copybytes = Math.min(remaining, MAX_CHUNKLENGTH);
            byte[] chunk = new byte[copybytes + 3];

            byte flags = 0;
            if (remaining <= MAX_CHUNKLENGTH) {
                flags |= 0x80; // last chunk
                if (count == 0) {
                    flags |= 0x40; // weird but true
                }
            } else if (count > 0) {
                flags |= 0x40; // consecutive chunk
            }

            chunk[0] = 0;
            chunk[1] = (byte) (flags | type);
            chunk[2] = (byte) (count & 0xff);

            System.arraycopy(data, count++ * MAX_CHUNKLENGTH, chunk, 3, copybytes);
            lastChunk = lastChunk == null ?
                    write(UUID_CHARACTERISTIC_CHUNKEDTRANSFER, chunk) :
                    lastChunk.flatMap(i -> write(UUID_CHARACTERISTIC_CHUNKEDTRANSFER, chunk));
            remaining -= copybytes;
        }
        return lastChunk;
    }

    private void handleDeviceEvent(byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }

        switch (value[0]) {
            case CALL_REJECT:
                Timber.i("call rejected");
//                callCmd.event = GBDeviceEventCallControl.Event.REJECT;
                break;
            case CALL_IGNORE:
                Timber.i("call ignored");
//                callCmd.event = GBDeviceEventCallControl.Event.IGNORE;
                break;
            case BUTTON_PRESSED:
                Timber.i("button pressed");
//                handleButtonEvent();
                break;
            case BUTTON_PRESSED_LONG:
                Timber.i("button long-pressed ");
//                handleLongButtonEvent();
                break;
            case START_NONWEAR:
                Timber.i("non-wear start detected");
//                processDeviceEvent(HuamiDeviceEvent.START_NONWEAR);
                break;
            case ALARM_TOGGLED:
                Timber.i("An alarm was toggled");
//                requestAlarms(builder);
                break;
            case FELL_ASLEEP:
                Timber.i("Fell asleep");
//                processDeviceEvent(HuamiDeviceEvent.FELL_ASLEEP);
                break;
            case WOKE_UP:
                Timber.i("Woke up");
//                processDeviceEvent(HuamiDeviceEvent.WOKE_UP);
                break;
            case STEPSGOAL_REACHED:
                Timber.i("Steps goal reached");
                break;
            case TICK_30MIN:
                Timber.i("Tick 30 min (?)");
                break;
            case FIND_PHONE_START:
                Timber.i("find phone started");
//                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
                break;
            case FIND_PHONE_STOP:
                Timber.i("find phone stopped");
//                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
                break;
            case MUSIC_CONTROL:
                Timber.i("got music control ");
/*
                GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();

                switch (value[1]) {
                    case 0:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case 1:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case 3:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case 4:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case 5:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case 6:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    case (byte) 224:
                        LOG.info("Music app started");
                        isMusicAppStarted = true;
                        sendMusicStateToDevice();
                        break;
                    case (byte) 225:
                        LOG.info("Music app terminated");
                        isMusicAppStarted = false;
                        break;
                    default:
                        LOG.info("unhandled music control event " + value[1]);
                        return;
                }
*/
                break;
            case MTU_REQUEST:
                int mtu = (value[2] & 0xff) << 8 | value[1] & 0xff;
                Timber.i("device announced MTU of %s", mtu);
                if (mtu < 23) {
                    Timber.i("Device announced unreasonable low MTU of " + mtu + ", ignoring");
                    break;
                }
                /*
                 * not really sure if this would make sense, is this event already a proof of a successful MTU
                 * negotiation initiated by the Huami device, and acknowledged by the phone? do we really have to
                 * requestMTU() from our side after receiving this?
                 * /
                if (mMTU != mtu) {
                    requestMTU(mtu);
                }
                */
                break;
            default:
                Timber.i("unhandled event %s", value[0]);
        }
    }

    private void showDiscoveredServices(RxBleDeviceServices s) {
        Timber.i("Discover services result");
        for (BluetoothGattService service : s.getBluetoothGattServices()) {
            Timber.i("    Svc: %s", service.getUuid());
            List<BluetoothGattService> includedServices = service.getIncludedServices();
            if (!includedServices.isEmpty()) {
                Timber.i("        Included");
                for (BluetoothGattService includedService : includedServices) {
                    Timber.i("            Svc: %s", includedService.getUuid());
                }
            }
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            if (!characteristics.isEmpty()) {
                Timber.i("        Characteristics");
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    Timber.i("            %s. Perm: %s, instID: %s, props: %s, wrType: %s", characteristic.getUuid(),
                            characteristic.getPermissions(), characteristic.getInstanceId(), characteristic.getProperties(), characteristic.getWriteType());
                    List<BluetoothGattDescriptor> characteristicDescriptors = characteristic.getDescriptors();
                    if (!characteristicDescriptors.isEmpty()) {
                        Timber.i("            Descriptors");
                        for (BluetoothGattDescriptor characteristicDescriptor : characteristicDescriptors) {
                            Timber.i("                Descriptor: %s, permissions: %s", characteristicDescriptor.getUuid(),
//                                    characteristicDescriptor.getInstanceId(),
                                    characteristicDescriptor.getPermissions());
                        }
                    }

                }
            }
        }
    }

    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return String.format("[%s]: %s", bytes.length, new String(hexChars));
    }
}

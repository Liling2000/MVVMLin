package com.liling.ble.manager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.liling.ble.TaskPriority;
import com.liling.ble.bean.RequestParam;
import com.liling.ble.bean.Task;
import com.liling.ble.bean.WriteTask;
import com.liling.ble.callback.BleScanDeviceCallBack;
import com.liling.ble.callback.OnBletoothCallBack;
import com.liling.ble.constant.BleConstant;
import com.liling.ble.listener.BleDataListener;
import com.liling.ble.listener.BleWriteDataStatueListener;
import com.liling.ble.presenter.BletoothImpl;
import com.liling.ble.queue.ConnectQueue;
import com.liling.ble.utils.BleUtils;
import com.liling.ble.utils.LogUtils;

import io.reactivex.rxjava3.core.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static com.liling.ble.constant.BleConstant.BleRelateTime.DELAY_TIME_500;
import static com.liling.ble.constant.BleConstant.BleRelateTime.WRITE_TIMEOUT;
import static com.liling.ble.constant.BleConstant.BleRelateTime.delayTime;
import static com.liling.ble.constant.BleConstant.BleRelateTime.scanTimeOut;
import static com.liling.ble.constant.BleConstant.publicParams.BLUETOOTH_CONNECT_PERMISSION_LOST;
import static com.liling.ble.constant.DeviceConstant.BleUuidKey.NOTIFY_CHARACTERRISTIC_UUID_KEY;
import static com.liling.ble.constant.DeviceConstant.BleUuidKey.SERVICE_UUID_KEY;
import static com.liling.ble.constant.DeviceConstant.BleUuidKey.WRITE_CHARACTERRISTIC_UUID_KEY;
import static com.liling.ble.constant.DeviceConstant.TaskFlag.MTU_WRITE;

/**
 * 蓝牙管理类
 *
 * @user lilingd
 * @Date 2021/1/26
 */
public class BleToolManager {
    public static final String TAG = "[BleSdk-Log]==========>";
    public static final String TAG_SCAN = "[ScanDevice-Log]==========>";
    private Context context;
    private OnBletoothCallBack bleImpl;
    //蓝牙管理类
    private BluetoothManager bluetoothManager;
    //蓝牙适配器
    private BluetoothAdapter bluetoothAdapter;
    //扫描对象
    private BluetoothLeScanner leScanner;
    //获取到的字节数组
    private byte[] bytesArray;
    //扫描到的设备列表
    private List<ScanResult> bleScanResultList = new ArrayList<>();
    //蓝牙数据获取监听接口
    private BleDataListener bleDataListener;
    //数据发送的结果回调
    private BleWriteDataStatueListener bleWriteDataListener;
    //根据mac地址绑定协议
    private Map<String, BluetoothGatt> gattMap = new ConcurrentHashMap<>();
    //写特征通道
    private Map<String, BluetoothGattCharacteristic> gattCharacteristicMap = new ArrayMap<>();
    //蓝牙mac地址/名称map
    private Map<String, String> bleSnNameMap = new ArrayMap<>();
    //服务UUID
    private String serviceUuidTemp = "";
    //写UUID
    private String writeUuidTemp = "";
    //通知UUID
    private String notifyUuidTemp = "";
    private static BleToolManager instance = null;
    private BleScanDeviceCallBack bleScanDeviceCallBack;
    //记录连接标识缓存
    private Map<String, Boolean> connectStateMap = new ConcurrentHashMap<>();
    //当前扫描设备名称
    private String currentDeviceName = "";
    private Handler handler = null;
    private MyScanCallback myScanCallback;
    //扫描到的设备mac地址集合
    private List<String> macList = new ArrayList<>();
    private List<String> allMatchDevices; //过滤设备列表
    private List<String> allSupportDevices; //所有支持的设备
    private boolean isAddedScanFilter; //标识扫描是否添加了过滤器
    //该map对象定义为每个设备所对应的各个UUID
    private Map<String, Map<String, String>> deviceUuidMap;
    private List<String> compatibleUuidDeviceList; //需兼容UUID的设备列表
    private Map<String, Integer> deviceCommandTimeOutMap = new HashMap<>(); //设备命令发送超时间隔 key/value->设备sn/命令发送超时时间
    private boolean isCloseScanLog; //扫描日志开关值 true:关闭 false:开启

    /**
     * 获取蓝牙管理器实例
     *
     * @return 管理类对象
     */
    public static BleToolManager getInstance() {
        if (instance == null) {
            instance = new BleToolManager();
        }
        return instance;
    }

    /**
     * 蓝牙初始化
     *
     * @param context 上下文引用
     */
    protected void init(Context context) {
        this.context = context;
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bleImpl = new BletoothImpl();
        handler = new Handler();
        myScanCallback = new MyScanCallback();
    }

    /**
     * 设置数据回调监听
     *
     * @param bleDataListener 数据监听
     */
    protected void setOnBleDataListener(BleDataListener bleDataListener) {
        this.bleDataListener = bleDataListener;
    }

    /**
     * 数据发送的结果回调
     *
     * @param bleWriteDataListener
     */
    protected void setOnBleWriteDataListener(BleWriteDataStatueListener bleWriteDataListener) {
        this.bleWriteDataListener = bleWriteDataListener;
    }

    /**
     * 设置扫描结果回调
     *
     * @param scanResultCallBack 扫描监听
     */
    protected void setOnBleScanResultCallBack(BleScanDeviceCallBack scanResultCallBack) {
        this.bleScanDeviceCallBack = scanResultCallBack;
    }

    /**
     * 设置需要兼容UUID的设备
     */
    protected void setCompatibleUuidDeviceList(List<String> compatibleUuidDeviceList) {
        this.compatibleUuidDeviceList = compatibleUuidDeviceList;
    }

    /**
     * 设置需过滤设备列表
     *
     * @param allMatchDevices 过滤设备列表
     */
    protected void setAllMatchDevices(List<String> allMatchDevices) {
        this.allMatchDevices = allMatchDevices;
    }

    /**
     * 获取过滤设备列表
     *
     * @return 过滤设备列表
     */
    public List<String> getAllMatchDevices() {
        return allMatchDevices;
    }

    /**
     * 获取所有支持的设备列表
     *
     * @return 所有设备列表
     */
    public List<String> getAllSupportDevices() {
        return allSupportDevices;
    }

    /**
     * 设置所有支持的设备列表
     *
     * @param allSupportDevices 所有设备集合
     */
    public void setAllSupportDevices(List<String> allSupportDevices) {
        this.allSupportDevices = allSupportDevices;
    }

    /**
     * 获取设备对应UUID(map存放)
     *
     * @return map对象
     */
    public Map<String, Map<String, String>> getDeviceUuidMap() {
        return deviceUuidMap;
    }

    /**
     * 设置map
     *
     * @param deviceUuidMap 设备对应UUID
     */
    public void setDeviceUuidMap(Map<String, Map<String, String>> deviceUuidMap) {
        this.deviceUuidMap = deviceUuidMap;
    }

    /**
     * 设置扫描日志开关状态
     *
     * @param closeScanLog true:关闭 false:开启
     */
    public void setCloseScanLog(boolean closeScanLog) {
        isCloseScanLog = closeScanLog;
    }

    /**
     * 判断蓝牙是否开启
     *
     * @return 是否开启蓝牙
     */
    protected boolean isBleOpen() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    /**
     * 开启蓝牙
     */
    protected void startBle() {
        bleImpl.startBle(bluetoothAdapter);
    }

    /**
     * 设置命令超时间隔
     *
     * @param sn      设备sn号
     * @param timeOut 超时间隔
     */
    protected boolean setCommandTimeOut(String sn, int timeOut) {
        if (deviceCommandTimeOutMap != null && !TextUtils.isEmpty(sn)) {
            deviceCommandTimeOutMap.put(sn.toLowerCase(), timeOut);
            return true;
        }
        return false;
    }

    /**
     * 开启全局扫描
     */
    protected void scanDevice() {
        scanDevice("", true);
    }

    /**
     * 扫描，
     *
     * @param isSetFilter 是否设置过滤器
     */
    protected void scanDevice(boolean isSetFilter) {
        scanDevice("", isSetFilter);
    }

    protected void setIsInOta(String sn, boolean isInOta) {
        PriorityQueueManager.getInstance().setOtaState(sn, isInOta);
    }

    /**
     * 扫描指定设备
     *
     * @param deviceName  设备名称
     * @param isSetFilter 是否设过滤器
     */
    protected void scanDevice(String deviceName, boolean isSetFilter) {
        ThreadPoolManager.getInstance().execute(() -> {
            if (!isBleOpen()) {
                return;
            }
            if (bleScanResultList != null) {
                bleScanResultList.clear();
                macList.clear();
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                return;
            }
            if (leScanner == null) {
                leScanner = bluetoothAdapter.getBluetoothLeScanner();
            }

            //5.0以上扫描
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (Exception e) {
                LogUtils.e("[cancelDiscovery]-->" + e.getMessage());
            }
            if (leScanner != null) {
                currentDeviceName = deviceName;
                isAddedScanFilter = isSetFilter;
                try {
                    leScanner.startScan(bleImpl.getScanFilters(deviceName, isSetFilter), bleImpl.getScanSettings(context, bluetoothAdapter), myScanCallback);
                    LogUtils.e(TAG + "开始扫描");
                } catch (Exception e) {
                    LogUtils.e(TAG + "[method:scanDevice]---" + e.getMessage());
                }
            }
        });
    }

    /**
     * 停止扫描
     *
     * @param isDelay 是否延迟停止(默认延迟10s)
     */
    protected void stopScanDelay(boolean isDelay) {
        //10s后停止扫描
        if (isBleOpen()) {
            if (isDelay) {
                handler.postDelayed(this::stopScanResolve, scanTimeOut);
            } else {
                stopScanResolve();
            }
        }
    }

    /**
     * 停止扫描的处理工作
     */
    private void stopScanResolve() {
        if (!isBleOpen()) {
            leScanner = null;
            return;
        }
        //停止扫描
        if (leScanner != null) {
            try {
                LogUtils.e(TAG + "扫描结束");
                leScanner.stopScan(myScanCallback);
            } catch (Exception e) {
                LogUtils.e(TAG + "[method:stopScanResolve]---" + e.getMessage());
            }
            leScanner = null;
        }
        if (bleScanDeviceCallBack != null) {
            bleScanDeviceCallBack.scanFinished(bleScanResultList);
        }
    }

    /**
     * 扫描回调内部类
     */
    private class MyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (TextUtils.isEmpty(currentDeviceName)) {
                //全局扫描
                if (!isAddedScanFilter) {
                    //未添加过滤器按支持的所有设备过滤
                    if (!bleImpl.isTheEquipmentValid(result.getScanRecord().getDeviceName())) {
                        return;
                    }
                }
                postMessage(result);
            } else {
                //单设备扫描结果
                postMessage(result);
            }
        }
    }

    /**
     * 回调扫描结果
     *
     * @param result 扫描结果
     */
    private void postMessage(ScanResult result) {
        if (result == null || result.getDevice() == null) {
            return;
        }
        if (!isCloseScanLog) {
            LogUtils.e(TAG_SCAN + "扫描到设备：" + result.getScanRecord().getDeviceName() + "/" + result.getDevice().getAddress());
        }
        if (bleScanDeviceCallBack != null) {
            bleScanDeviceCallBack.scanDevice(result);
        }
        addDevice(result);
    }

    /**
     * 连接蓝牙(连接请求放队列，防止同时请求连接阻塞蓝牙)
     *
     * @param mBluetoothDevice 蓝牙对象
     * @param model            蓝牙名称
     */
    protected void connectBleDevice(BluetoothDevice mBluetoothDevice, String model) {
        if (!isOpenBluetoothConnectPermission()) {
            LogUtils.e(TAG + "BLUETOOTH_CONNECT权限缺失");
            return;
        }
        if (mBluetoothDevice == null || !isBleOpen() || getConnectedState(mBluetoothDevice) == STATE_CONNECTED) {
            return;
        }
        try {
            if (connectStateMap.containsKey(mBluetoothDevice.getAddress()) && connectStateMap.get(mBluetoothDevice.getAddress()))
                return;
        } catch (Exception e) {
            LogUtils.e(TAG + "[method:connectBleDevice]" + e.getMessage());
            return;
        }
        connectStateMap.put(mBluetoothDevice.getAddress(), true);
        connect(mBluetoothDevice, model);
    }

    /**
     * 连接请求存放队列
     *
     * @param mBluetoothDevice 设备对象
     * @param model            ble名
     */
    protected void connectWithQueue(BluetoothDevice mBluetoothDevice, String model) {
        if (!isOpenBluetoothConnectPermission()) {
            LogUtils.e(TAG + "BLUETOOTH_CONNECT权限缺失");
            return;
        }
        if (mBluetoothDevice == null || !isBleOpen()) {
            return;
        }
        try {
            if (connectStateMap.containsKey(mBluetoothDevice.getAddress()) && connectStateMap.get(mBluetoothDevice.getAddress()))
                return;
        } catch (Exception e) {
            LogUtils.e(TAG + "[method:connectBleDevice]" + e.getMessage());
            return;
        }
        //正在连接
        connectStateMap.put(mBluetoothDevice.getAddress(), true);
        Task task = new Task(new RequestParam.Builder().model(model).device(mBluetoothDevice).build());
        ConnectQueue.getInstance().put(task);
        LogUtils.e(TAG + "设备连接请求入队列---" + "model:" + model + "&sn:" + mBluetoothDevice.getAddress());
    }

    /**
     * 连接请求不存放队列
     *
     * @param mBluetoothDevice 蓝牙设备
     * @param model            设备名称
     */
    protected void connectNoQueue(BluetoothDevice mBluetoothDevice, String model) {
        connectBleDevice(mBluetoothDevice, model);
    }

    /**
     * 连接请求
     *
     * @param model            蓝牙名称
     * @param mBluetoothDevice 蓝牙设备对象
     */
    public void connect(BluetoothDevice mBluetoothDevice, String model) {
        BluetoothGatt bluetoothGatt;
        try {
            bluetoothGatt = gattMap.get(mBluetoothDevice.getAddress());
            if (!TextUtils.isEmpty(model)) {
                if (!bleSnNameMap.containsKey(mBluetoothDevice.getAddress())) {
                    bleSnNameMap.put(mBluetoothDevice.getAddress(), model);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG + "[method:connect]" + e.getMessage());
            return;
        }

        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        LogUtils.e(TAG + "设备请求连接---" + "model:" + model + "&sn:" + mBluetoothDevice.getAddress());
        mBluetoothDevice.connectGatt(context, false, new MyBluetoothGattCallback(), BluetoothDevice.TRANSPORT_LE);
    }

    /**
     * 连接异常、超时等非正常连接处理
     *
     * @param deviceAddress 设备地址
     */
    private void resolveConnectException(BluetoothGatt gatt, String deviceAddress) {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
        }
        connectStateMap.remove(deviceAddress);
        gattMap.remove(deviceAddress);
        ThreadPoolManager.getInstance().executeOnMainThread(() -> {
            if (bleDataListener != null && gatt != null) {
                bleDataListener.sendConnectState(gatt.getDevice(), BleConstant.BleConnectState.stateDisconnected);
                ConnectQueue.getInstance().setBusy(false);
                PriorityQueueManager.getInstance().deleteQueue(deviceAddress);
            }
        });
    }

    /**
     * 连接蓝牙
     *
     * @param sn    mac地址
     * @param model 设备名称
     */
    protected void connectBleDeviceBySn(String sn, String model) {
        if (BluetoothAdapter.checkBluetoothAddress(sn)) {
            connectBleDevice(bluetoothAdapter.getRemoteDevice(sn), model);
        }
    }

    /**
     * 修改MTU
     *
     * @param sn  mac地址
     * @param mtu mtu大小
     */
    protected void requestMtu(String sn, int mtu) {
        requstMtu(sn, mtu, TaskPriority.DEFAULT_PRIORITY);
    }


    /**
     * 修改mtu
     *
     * @param sn       设备sn号
     * @param mtu      mtu值
     * @param priority 任务优先级
     */
    protected void requstMtu(String sn, int mtu, @TaskPriority int priority) {
        if (!TextUtils.isEmpty(sn)) {
            WriteTask writeTask = new WriteTask();
            writeTask.setRequestParam(new RequestParam.Builder().address(sn).mtu(mtu).model(getDeviceNameBySn(sn)).taskFlag(MTU_WRITE).build());
            writeTask.setPriorityLevel(priority);
            PriorityQueueManager.getInstance().addDataToQueue(sn, writeTask, getCommandTimeOutBySn(sn));
        }
    }

    /**
     * 通过sn获取每台设备对应的命令延时值
     *
     * @param sn 设备sn号
     * @return 设备命令发送超时时间
     */
    private int getCommandTimeOutBySn(String sn) {
        if (deviceCommandTimeOutMap != null && !TextUtils.isEmpty(sn) && deviceCommandTimeOutMap.containsKey(sn.toLowerCase()))
            return deviceCommandTimeOutMap.get(sn.toLowerCase());
        return WRITE_TIMEOUT;
    }

    /**
     * 执行mtu申请任务
     *
     * @param sn  mac地址
     * @param mtu mtu值
     */
    public void excuteMtuTask(String sn, int mtu) {
        if (gattMap != null) {
            BluetoothGatt gatt = gattMap.get(sn);
            if (gatt != null) {
                LogUtils.e(TAG + "Mtu扩容请求---mtu:" + mtu);
                gatt.requestMtu(mtu);
            }
        }
    }

    /**
     * 读取蓝牙信号
     *
     * @param sn 蓝牙mac地址
     */
    protected void readRemoteRssi(String sn) {
        if (gattMap != null) {
            BluetoothGatt gatt = gattMap.get(sn);
            if (gatt != null) {
                gatt.readRemoteRssi();
            }
        }
    }

    /**
     * 蓝牙断连
     *
     * @param mBluetoothDevice 蓝牙对象
     * @param model            蓝牙名称
     */
    protected void disconnectBle(BluetoothDevice mBluetoothDevice, String model) {
        if (mBluetoothDevice == null || bluetoothAdapter == null || gattMap == null || gattMap.isEmpty()) {
            return;
        }
//        if (!isOpenBluetoothConnectPermission()) {
//            return;
//        }
        if (!TextUtils.isEmpty(model)) {
            bleSnNameMap.remove(mBluetoothDevice.getAddress());
        }
        BluetoothGatt mBlueGatt = gattMap.get(mBluetoothDevice.getAddress());
        if (mBlueGatt == null) {
            LogUtils.e(TAG + "mBlueGatt is null");
            return;
        }
        LogUtils.e(TAG + "设备断连请求---model:" + model + "&sn:" + mBluetoothDevice.getAddress());
        mBlueGatt.disconnect();
    }

    /**
     * 蓝牙断连
     *
     * @param sn    mac地址
     * @param model 蓝牙名称
     */
    protected void disconnectBle(String sn, String model) {
        if (TextUtils.isEmpty(sn) || bluetoothAdapter == null || gattMap == null) {
            return;
        }
//        if (!isOpenBluetoothConnectPermission()) {
//            return;
//        }
        if (!TextUtils.isEmpty(model)) {
            bleSnNameMap.remove(sn);
        }
        BluetoothGatt mBlueGatt = gattMap.get(sn);
        if (mBlueGatt == null) {
            LogUtils.e(TAG + "mBlueGatt is null");
            return;
        }
        LogUtils.e(TAG + "设备断连请求---model:" + model + "&sn:" + sn);
        mBlueGatt.disconnect();
        gattMap.remove(sn);
    }

    /**
     * 断连所有设备
     */
    protected void clearAllConnectDevice() {
        if (isBleOpen()) {
            if (gattMap != null && !gattMap.isEmpty()) {
                LogUtils.e(TAG + "蓝牙全部断连");
                List<BluetoothGatt> allDeviceGattList = new ArrayList(gattMap.values());
                for (BluetoothGatt bluetoothGatt : allDeviceGattList) {
                    if (bluetoothGatt != null) {
                        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(bluetoothGatt.getDevice().getAddress());
                        if (bluetoothDevice != null) {
                            handler.postDelayed(() -> disconnectBle(bluetoothDevice, bluetoothDevice.getName()), delayTime);
                        }
                    }
                }
            }
        }
        //加延迟，优先满足设备全部断连
        handler.postDelayed(() -> clearCacheData(), delayTime);
    }

    /**
     * 列表清空
     */
    protected void clearCacheData() {
        //清空所有缓存数据
        bleScanResultList.clear();
        gattMap.clear();
        gattCharacteristicMap.clear();
        bleSnNameMap.clear();
        connectStateMap.clear();
        deviceCommandTimeOutMap.clear();
        macList.clear();

        ConnectQueue.getInstance().clear();
        PriorityQueueManager.getInstance().clear();
    }

    /**
     * 清空蓝牙相关缓存信息(蓝牙关闭情形使用)
     */
    protected void cleanBleRelateData() {
        if (gattMap != null) {
            gattMap.clear();
        }
        if (gattCharacteristicMap != null) {
            gattCharacteristicMap.clear();
        }
        if (bleSnNameMap != null) {
            bleSnNameMap.clear();
        }
        if (connectStateMap != null) {
            connectStateMap.clear();
        }
    }


    /**
     * 写数据
     *
     * @param data       写入数据
     * @param macAddress 蓝牙mac地址
     */
    protected void writeBleData(byte[] data, String macAddress) {
        executeWriteCharacteristic(data, macAddress);
    }

    /**
     * 写数据存放队列
     *
     * @param data       写入数据
     * @param macAddress 蓝牙mac地址
     */
    protected void writeDataWithQueue(byte[] data, String macAddress) {
        writeDataWithQueue(data, macAddress, TaskPriority.DEFAULT_PRIORITY);
    }

    /**
     * 写数据存放队列
     *
     * @param data         写入数据
     * @param sn           蓝牙mac地址
     * @param taskPriority 任务优先级
     */
    protected void writeDataWithQueue(byte[] data, String sn, @TaskPriority int taskPriority) {
        if (bluetoothAdapter == null || !isOpenBluetoothConnectPermission() || !BluetoothAdapter.checkBluetoothAddress(sn)) {
            return;
        }
        if (getConnectedState(bluetoothAdapter.getRemoteDevice(sn)) == STATE_CONNECTED && !TextUtils.isEmpty(sn)) {
            WriteTask writeTask = new WriteTask();
            writeTask.setRequestParam(new RequestParam.Builder().address(sn).data(data).model(getDeviceNameBySn(sn)).build());
            writeTask.setPriorityLevel(taskPriority);
            PriorityQueueManager.getInstance().addDataToQueue(sn, writeTask, getCommandTimeOutBySn(sn));
            LogUtils.e(TAG + "1.入队列命令:" + BleUtils.byte2hex(data) + "---sn:" + sn);
        }
    }

    /**
     * 写数据不存放队列
     *
     * @param data       写入数据
     * @param macAddress 蓝牙mac地址
     */
    protected void writeDataNoQueue(byte[] data, String macAddress) {
        executeWriteCharacteristic(data, macAddress);
    }

    /**
     * 执行写任务
     *
     * @param data       写数据
     * @param macAddress 设备mac地址
     */
    public void executeWriteCharacteristic(byte[] data, String macAddress) {
        BluetoothGattCharacteristic mWriteCharacteristic = gattCharacteristicMap.get(macAddress);
        BluetoothGatt mBluetoothGatt = gattMap.get(macAddress);
        if (mBluetoothGatt == null || mWriteCharacteristic == null) {
            return;
        }
        if (!isOpenBluetoothConnectPermission()) {
            return;
        }
        mWriteCharacteristic.setValue(data);
        try {
            LogUtils.e(TAG + "[writeBleData]---data:" + BleUtils.byte2hex(data) + "---sn:" + macAddress);
            boolean result = mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
            LogUtils.e(TAG + "---sn:" + macAddress + "---数据写入状态:" + result);
            if (bleWriteDataListener != null) {
                bleWriteDataListener.onWriteDataStatue(macAddress, result, data);
            }
        } catch (Exception e) {
            LogUtils.e(TAG + "[method:writeBleData]---exception:" + e.getMessage());
        }
    }

    /**
     * 写数据 设置无响应
     *
     * @param data       写入字节数组
     * @param macAddress 蓝牙地址
     */
    protected void writeBleDataWithNoResponse(byte[] data, String macAddress) {
        ThreadPoolManager.getInstance().execute(() -> {
            BluetoothGattCharacteristic mWriteCharacteristic = gattCharacteristicMap.get(macAddress);
            BluetoothGatt mBluetoothGatt = gattMap.get(macAddress);
            if (mBluetoothGatt == null || mWriteCharacteristic == null) {
                return;
            }
            mWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mWriteCharacteristic.setValue(data);
            try {
                LogUtils.e("[writeBleDataWithNoResponse]---data:" + BleUtils.byte2hex(data) + "---sn:" + macAddress);
                mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
            } catch (Exception e) {
                LogUtils.e(TAG + "[method:writeBleData]---exception:" + e.getMessage());
            }
        });
    }

    /**
     * 执行写任务
     *
     * @param data       写数据
     * @param macAddress 设备mac地址
     */
    public void executeWriteCharacteristicByUuid(byte[] data, String macAddress, String serviceId, String writeUuid) {
        BluetoothGatt mBluetoothGatt = gattMap.get(macAddress);
        if (mBluetoothGatt == null) {
            LogUtils.e(TAG + "[writeBleData by uuid]---can not found BluetoothGatt:---sn:" + macAddress);
            return;
        }
        if (!isOpenBluetoothConnectPermission()) {
            LogUtils.e(TAG + "[writeBleData by uuid]---missing permissions :---sn:" + macAddress);
            return;
        }
        //获取通信服务
        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceId));
        if (gattService == null) {
            LogUtils.e(TAG + "[writeBleData by uuid]---can not found gattService :---serviceId:" + serviceId);
            return;
        }
        BluetoothGattCharacteristic mWriteCharacteristic = gattService.getCharacteristic(UUID.fromString(writeUuid));
        if (mWriteCharacteristic == null) {
            LogUtils.e(TAG + "[writeBleData by uuid]---can not found BluetoothGattCharacteristic :---writeUuid:" + writeUuid);
            return;
        }
        mWriteCharacteristic.setValue(data);
        try {
            LogUtils.e(TAG + "[writeBleData by uuid]---data:" + BleUtils.byte2hex(data) + "---sn:" + macAddress);
            boolean result = mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
            LogUtils.e(TAG + "---sn:" + macAddress + "---数据写入状态:" + result);
            if (bleWriteDataListener != null) {
                bleWriteDataListener.onWriteDataStatue(macAddress, result, data);
            }
        } catch (Exception e) {
            LogUtils.e(TAG + "[method:writeBleData by uuid]---exception:" + e.getMessage());
        }
    }

    public void executeReadCharacteristicByUuid(String macAddress, String serviceId, String readUuid) {
        BluetoothGatt mBluetoothGatt = gattMap.get(macAddress);
        if (mBluetoothGatt == null) {
            LogUtils.e(TAG + "[readBleData by uuid]---can not found BluetoothGatt:---sn:" + macAddress);
            return;
        }
        if (!isOpenBluetoothConnectPermission()) {
            LogUtils.e(TAG + "[readBleData by uuid]---missing permissions :---sn:" + macAddress);
            return;
        }
        //获取通信服务
        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceId));
        if (gattService == null) {
            LogUtils.e(TAG + "[readBleData by uuid]---can not found gattService :---serviceId:" + serviceId);
            return;
        }
        BluetoothGattCharacteristic mReadCharacteristic = gattService.getCharacteristic(UUID.fromString(readUuid));
        if (mReadCharacteristic == null) {
            LogUtils.e(TAG + "[readBleData by uuid]---can not found BluetoothGattCharacteristic :---readUuid:" + readUuid);
            return;
        }
        try {
            boolean result = mBluetoothGatt.readCharacteristic(mReadCharacteristic);
            LogUtils.e(TAG + "---sn:" + macAddress + "---数据写入状态:" + result);
            if (bleWriteDataListener != null) {
                bleWriteDataListener.onWriteDataStatue(macAddress, result, null);
            }
        } catch (Exception e) {
            LogUtils.e(TAG + "[method:readBleData by uuid]---exception:" + e.getMessage());
        }
    }

    public void setBleNotifyCharacteristicByUuid(String macAddress, String serviceId, String notifyUuid) {
        BluetoothGatt mBluetoothGatt = gattMap.get(macAddress);
        if (mBluetoothGatt == null) {
            LogUtils.e(TAG + "[setNotify by uuid]---can not found BluetoothGatt:---sn:" + macAddress);
            return;
        }
        if (!isOpenBluetoothConnectPermission()) {
            LogUtils.e(TAG + "[setNotify by uuid]---missing permissions :---sn:" + macAddress);
            return;
        }
        //获取通信服务
        BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(serviceId));
        if (gattService == null) {
            LogUtils.e(TAG + "[setNotify by uuid]---can not found gattService :---serviceId:" + serviceId);
            return;
        }
        BluetoothGattCharacteristic mNotifyCharacteristic = gattService.getCharacteristic(UUID.fromString(notifyUuid));
        if (mNotifyCharacteristic == null) {
            LogUtils.e(TAG + "[setNotify by uuid]---can not found BluetoothGattCharacteristic :---notifyUuid:" + notifyUuid);
            return;
        }

        boolean isEnableNotify = mBluetoothGatt.setCharacteristicNotification(mNotifyCharacteristic, true);

        LogUtils.e(TAG + "[setNotify by uuid]---isEnableNotify :" + isEnableNotify);
        //判断通知特征值是否设置成功
        if (isEnableNotify) {
            //获取描述列表
            List<BluetoothGattDescriptor> descriptorList = mNotifyCharacteristic.getDescriptors();
            for (BluetoothGattDescriptor descriptor : descriptorList) {
                //为特征值中的描述对象设置广播监听
                boolean enableNotify = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (enableNotify) {
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }
    }


    /**
     * 蓝牙连接、读写等相关回调内部类
     */
    class MyBluetoothGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
            ThreadPoolManager.getInstance().execute(() -> {
                if (gatt == null || gatt.getDevice() == null) {
                    return;
                }
                BluetoothDevice device = gatt.getDevice();
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                LogUtils.e(TAG + "设备连接状态---model:" + deviceName + "&sn:" + deviceAddress + "&status:" + status + "&newState:" + newState);
                //操作成功
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //判断连接码
                    //设备连接成功
                    if (newState == STATE_CONNECTED) {
                        LogUtils.e(TAG + "[method:onConnectionStateChange]---开始扫描服务model:" + deviceName + "&sn:" + deviceAddress);
                        gattMap.put(deviceAddress, gatt);
                        //扫描服务
                        handler.postDelayed(gatt::discoverServices, delayTime);
//                        gatt.discoverServices();
                    } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                        LogUtils.e(TAG + "设备正在连接中 model:" + deviceName + "&sn:" + deviceAddress);
                        ThreadPoolManager.getInstance().executeOnMainThread(() -> {
                            if (bleDataListener != null) {
                                bleDataListener.sendConnectState(device, BleConstant.BleConnectState.stateConnecting);
                            }
                        });
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTING) {
                        LogUtils.e(TAG + "设备正在断开连接 model:" + deviceName + "&sn:" + deviceAddress);
                        ThreadPoolManager.getInstance().executeOnMainThread(() -> {
                            if (bleDataListener != null) {
                                bleDataListener.sendConnectState(device, BleConstant.BleConnectState.stateConnecting);
                            }
                        });
                    } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                        //主动断连
                        LogUtils.e(TAG + "设备已断开连接 model:" + deviceName + "&sn:" + deviceAddress);
                        resolveConnectException(gatt, deviceAddress);
                    }
                } else {
                    LogUtils.e(TAG + "设备异常断开连接 model:" + deviceName + "&sn:" + deviceAddress);
                    resolveConnectException(gatt, deviceAddress);
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null && gatt.getDevice() != null) {
                LogUtils.e(TAG + "开始获取服务 model:" + gatt.getDevice().getName() + "&sn:" + gatt.getDevice().getAddress());
                if (bleSnNameMap.containsKey(gatt.getDevice().getAddress())) {
                    resetUuid(bleSnNameMap.get(gatt.getDevice().getAddress()), gatt);
                }
                LogUtils.e(TAG + "[serviceuuid]:" + serviceUuidTemp);
                LogUtils.e(TAG + "[writeuuid]:" + writeUuidTemp);
                LogUtils.e(TAG + "[notifyuuid]:" + notifyUuidTemp);
                try {
                    //获取通信服务
                    BluetoothGattService gattService = gatt.getService(UUID.fromString(serviceUuidTemp));
                    if (gattService == null) {
                        LogUtils.e(TAG + "[未获取到gattservice]");
                        return;
                    }
                    //获取通知特征
                    setBleNotifyCharactor(gatt, gattService);
                    LogUtils.e(TAG + "Notify特征通道已建立");
                    //获取写特征
                    setWriteCharactor(gatt, gattService);
                    LogUtils.e(TAG + "Write特征通道已建立");
                } catch (Exception e) {
                    LogUtils.e(TAG + "[method:onServicesDiscovered]---exception:" + e.getMessage());
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (gatt != null && gatt.getDevice() != null) {
                LogUtils.e(TAG + "[method:onCharacteristicRead]---model:" + gatt.getDevice().getName() + "&sn:" + gatt.getDevice().getAddress() + "&status:" + status + "value: " + BleUtils.byteToString(characteristic.getValue()));
                byte[] value = characteristic.getValue();
                if (bleDataListener != null && value != null) {
                    bleDataListener.onBleNotify(gatt.getDevice(), value);
                }
            }
        }

        //写入成功后的回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (gatt != null && gatt.getDevice() != null) {
                LogUtils.e(TAG + "[method:onCharacteristicWrite]---发送成功:model:" + gatt.getDevice().getName() + "&sn:" + gatt.getDevice().getAddress() + "&status:" + status);
            }
        }

        //接收数据的回调
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (gatt == null || gatt.getDevice() == null) {
                return;
            }
            LogUtils.e(TAG + "[method:onCharacteristicChanged]---获取到设备数据:model:" + gatt.getDevice().getName() + "&sn:" + gatt.getDevice().getAddress());
            // value为设备发送的数据，根据数据协议进行解析
            bytesArray = characteristic.getValue();
            if (bleDataListener != null && bytesArray != null) {
                bleDataListener.onBleNotify(gatt.getDevice(), bytesArray);

                //该判断逻辑是针对于业务请求头对应的唯一性判断(不适用于数据返回字符串内容相关命令请求)
                try {
                    String responseData = BleUtils.byteToString(characteristic.getValue());
                    RequestParam requestParam = PriorityQueueManager.getInstance().getRequestParamBySn(gatt.getDevice().getAddress());
                    if (!TextUtils.isEmpty(responseData) && responseData.length() >= 4 && requestParam != null && !TextUtils.isEmpty(requestParam.getResponsePrefix())) {
                        if (requestParam.getResponsePrefix().equals(responseData.substring(0, 4))) {
                            //任务执行结束
                            PriorityQueueManager.getInstance().setQueueRunningState(gatt.getDevice().getAddress(), false);
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG + "[method:onCharacteristicChanged]" + e.getMessage());
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            //因为BLE发现服务和设置特征、通知等是需要耗时的，所以不能连接成功后立马发送数据，可以等到在
            //onDescriptorWrite()回调时，或者手动延迟一段时间再去做发送操作
            LogUtils.e(TAG + "[method:onDescriptorWrite]---status:" + status);
            ThreadPoolManager.getInstance().executeOnMainThread(() -> {
                if (bleDataListener != null && gatt != null) {
                    bleDataListener.sendConnectState(gatt.getDevice(), BleConstant.BleConnectState.stateConnected);
                    LogUtils.e(TAG + "设备已连接");
                    ConnectQueue.getInstance().setBusy(false);
                }
            });
            Observable.timer(DELAY_TIME_500, TimeUnit.MILLISECONDS).subscribe(aLong -> {
                if (bleDataListener != null && gatt != null) {
                    bleDataListener.sendWriteMsg(gatt.getDevice(), status);
                }
            });
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            PriorityQueueManager.getInstance().setQueueRunningState(gatt.getDevice().getAddress(), false);
            LogUtils.e(TAG + "[method:onMtuChanged]---status:" + status + "&mtu:" + mtu);
            if (bleDataListener != null) {
                bleDataListener.mtuResponse(gatt.getDevice().getAddress(), status, mtu);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (gatt != null && gatt.getDevice() != null) {
                LogUtils.e(TAG + "[method:onReadRemoteRssi]" + "---name:" + gatt.getDevice().getName() + "&sn:" + gatt.getDevice().getAddress() + "&rssi:" + rssi);
                bleDataListener.onRssiResponse(gatt.getDevice().getAddress(), gatt.getDevice().getName(), rssi);
            }
        }
    }

    /**
     * 根据不同的蓝牙设备配置对应特征值
     *
     * @param deviceName 设备名称
     */
    private void resetUuid(String deviceName, BluetoothGatt gatt) {
        if (deviceUuidMap != null && !deviceUuidMap.isEmpty()) {
            Map<String, String> uuidMap = deviceUuidMap.get(deviceName);
            if (uuidMap != null && !uuidMap.isEmpty()) {
                serviceUuidTemp = uuidMap.get(SERVICE_UUID_KEY);
                writeUuidTemp = uuidMap.get(WRITE_CHARACTERRISTIC_UUID_KEY);
                notifyUuidTemp = uuidMap.get(NOTIFY_CHARACTERRISTIC_UUID_KEY);
                if (compatibleUuidDeviceList != null && !compatibleUuidDeviceList.isEmpty() && compatibleUuidDeviceList.contains(deviceName)) {
                    //需做UUID的兼容处理
                    if (gatt != null && gatt.getServices() != null && !gatt.getServices().isEmpty()) {
                        serviceUuidTemp = gatt.getServices().get(0).getUuid().toString();
                        List<BluetoothGattCharacteristic> bluetoothGattCharacteristicList = gatt.getServices().get(0).getCharacteristics();
                        if (bluetoothGattCharacteristicList != null && bluetoothGattCharacteristicList.size() >= 2) {
                            notifyUuidTemp = bluetoothGattCharacteristicList.get(0).getUuid().toString();
                            writeUuidTemp = bluetoothGattCharacteristicList.get(1).getUuid().toString();
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置通知特征通道
     *
     * @param gatt        协议
     * @param gattService 蓝牙服务
     */
    private void setBleNotifyCharactor(BluetoothGatt gatt, BluetoothGattService gattService) {
        BluetoothGattCharacteristic notifyCharacteristic = gattService.getCharacteristic(UUID.fromString(notifyUuidTemp));
        boolean isEnableNotify = gatt.setCharacteristicNotification(notifyCharacteristic, true);
        //判断通知特征值是否设置成功
        if (isEnableNotify) {
            //获取描述列表
            List<BluetoothGattDescriptor> descriptorList = notifyCharacteristic.getDescriptors();
            for (BluetoothGattDescriptor descriptor : descriptorList) {
                //为特征值中的描述对象设置广播监听
                boolean enableNotify = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (enableNotify) {
                    gatt.writeDescriptor(descriptor);
                }
            }
        }
    }

    /**
     * 设置写通道
     *
     * @param gatt        蓝牙协议
     * @param gattService 蓝牙服务
     */
    private void setWriteCharactor(BluetoothGatt gatt, BluetoothGattService gattService) {
        BluetoothGattCharacteristic writeCharacteristic = gattService.getCharacteristic(UUID.fromString(writeUuidTemp));
        gattCharacteristicMap.put(gatt.getDevice().getAddress(), writeCharacteristic);
    }

    /**
     * 本地集合添加蓝牙设备
     *
     * @param scanResult 蓝牙设备
     */
    private void addDevice(ScanResult scanResult) {
        String mac = scanResult.getDevice().getAddress();
        if (bleScanResultList.size() <= 0) {
            bleScanResultList.add(scanResult);
            macList.add(mac);
            if (bleScanDeviceCallBack != null) {
                bleScanDeviceCallBack.scanFirstDevice(scanResult);
            }
        } else {
            if (!macList.contains(mac)) {
                bleScanResultList.add(scanResult);
                macList.add(mac);
            }
        }
    }

    /**
     * 获取扫描添加设备列表
     *
     * @return 蓝牙设备列表
     */
    protected List<ScanResult> getBluetoothDeviceList() {
        return bleScanResultList;
    }

    /**
     * 获取设备当前连接状态
     *
     * @param device 当前设备
     * @return 连接状态
     */
    private int getConnectedState(BluetoothDevice device) {
        int state = 100;

        if (!isOpenBluetoothConnectPermission()) {
            return state;
        }

        if (bluetoothManager != null && device != null && isBleOpen()) {
            state = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        }
        return state;
    }

    /**
     * 检测是否在android12上开启了BluetoothConnect权限 低于12不需要检测该权限
     */
    private boolean isOpenBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //android 12 需检查BLUETOOTH_CONNECT权限
            if (context != null && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                //发送Bluetooth_connect缺失广播
                context.sendBroadcast(new Intent(BLUETOOTH_CONNECT_PERMISSION_LOST));
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当前设备连接状态
     *
     * @param sn mac地址
     * @return 连接状态
     */
    public boolean getConnectedState(String sn) {
        if (BluetoothAdapter.checkBluetoothAddress(sn)) {
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(sn);
            return getConnectedState(bluetoothDevice) == STATE_CONNECTED;
        }
        return false;
    }

    /**
     * 获取蓝牙adapter
     *
     * @return 蓝牙适配器
     */
    protected BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * 获取远程设备对象
     *
     * @param sn 设备sn
     * @return 设备对象
     */
    private BluetoothDevice getRemoteDevice(String sn) {
        if (TextUtils.isEmpty(sn) || bluetoothAdapter == null || !BluetoothAdapter.checkBluetoothAddress(sn)) {
            return null;
        }
        return bluetoothAdapter.getRemoteDevice(sn);
    }

    /**
     * 通过设备sn获取其对应名称
     *
     * @param sn 设备sn号
     * @return 设备名称
     */
    private String getDeviceNameBySn(String sn) {
        BluetoothDevice device = getRemoteDevice(sn);
        if (device != null) {
            return device.getName();
        }
        return "";
    }

    public Context getContext() {
        return context;
    }

    /**
     * 释放接口对象
     */
    public void release() {
        bleDataListener = null;
        bleScanDeviceCallBack = null;
        handler.removeCallbacksAndMessages(null);
    }
}

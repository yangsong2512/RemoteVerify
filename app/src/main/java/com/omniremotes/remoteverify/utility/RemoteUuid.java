package com.omniremotes.remoteverify.utility;

public enum RemoteUuid {
    OMNI_VOICE_SERVICE_UUID("65fa9513-e8ca-4efe-b5ba-67b7c44101dc"),
    OMNI_VOICE_CONTROL_UUID("20d695c7-1f7b-4d11-afec-c6c5cfae7f52"),
    OMNI_VOICE_DATA_UUID("9c98fa55-3de4-4361-8f26-ba1c62c8f222"),
    OMNI_OTA_SERVICE_UUID("cbc0e185-76af-402e-9b82-620884e57934"),
    OMNI_OTA_CMD_UUID("0f3eabd8-c687-42fc-adcf-208bc2c126b9"),
    OMNI_OTA_IMG_BLK_UUID("83573389-10fc-416a-b451-8be01e37442c"),
    ATVV_SERVICE_UUID("AB5E0001-5A21-4F05-BC7D-AF01F617B664"),
    ATVV_CHAR_TX("AB5E0002-5A21-4F05-BC7D-AF01F617B664"),
    ATVV_CHAR_RX("AB5E0003-5A21-4F05-BC7D-AF01F617B664"),
    ATVV_CHAR_CTL("AB5E0004-5A21-4F05-BC7D-AF01F617B664"),
    CLIENT_CHARACTERISTIC_CONFIG("00002902-0000-1000-8000-00805F9B34FB");
    RemoteUuid(String uuid){
        this.value = uuid;
    }
    String value(){
        return value;
    }
    String value;
}

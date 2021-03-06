package com.intellij.flex.uiDesigner.io {
import flash.utils.ByteArray;
import flash.utils.IDataInput;

public final class AmfUtil {
  public static function readUInt29(input:IDataInput):int {
    var b:int = input.readByte() & 0xFF;
    if (b < 128) {
      return b;
    }

    var value:int = (b & 0x7F) << 7;
    if ((b = input.readByte() & 0xFF) < 128) {
      return value | b;
    }

    value = (value | (b & 0x7F)) << 7;
    if ((b = input.readByte() & 0xFF) < 128) {
      return value | b;
    }

    return ((value | (b & 0x7F)) << 8) | (input.readByte() & 0xFF);
  }

  public static function readString(input:IDataInput):String {
    return input.readUTFBytes(readUInt29(input));
  }

  public static function readNullableString(input:IDataInput):String {
    const l:int = readUInt29(input);
    return l == 0 ? null : input.readUTFBytes(l);
  }

  public static function readByteArray(input:IDataInput):ByteArray {
    var r:ByteArray = new ByteArray();
    input.readBytes(r, 0, readUInt29(input));
    return r;
  }
}
}
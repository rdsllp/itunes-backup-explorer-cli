module hearsay.idevice_decryption {
  requires dd.plist;
  requires org.bouncycastle.provider;
  requires java.sql;
  requires org.xerial.sqlitejdbc;
  requires org.slf4j;
  requires org.slf4j.simple;

  exports hearsay.idevice_decryption;
  exports hearsay.idevice_decryption.api;
  exports hearsay.idevice_decryption.util;
}

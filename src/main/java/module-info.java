module me.maxih.itunes_backup_explorer {
  requires dd.plist;
  requires org.bouncycastle.provider;
  requires java.sql;
  requires org.xerial.sqlitejdbc;
  requires org.slf4j;
  requires org.slf4j.simple;

  exports me.maxih.itunes_backup_explorer;
  exports me.maxih.itunes_backup_explorer.api;
  exports me.maxih.itunes_backup_explorer.util;
}

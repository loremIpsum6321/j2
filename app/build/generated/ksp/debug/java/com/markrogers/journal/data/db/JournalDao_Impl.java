package com.markrogers.journal.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class JournalDao_Impl implements JournalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EntryEntity> __insertionAdapterOfEntryEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public JournalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEntryEntity = new EntityInsertionAdapter<EntryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `entries` (`id`,`createdAt`,`title`,`body`,`moodRating`,`moodEmojisCsv`,`toggleX`,`toggleY`,`toggleZ`,`toggleW`,`sleepMinutes`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EntryEntity entity) {
        statement.bindLong(1, entity.getId());
        final Long _tmp = __converters.fromInstant(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, _tmp);
        }
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getBody());
        if (entity.getMoodRating() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getMoodRating());
        }
        statement.bindString(6, entity.getMoodEmojisCsv());
        final int _tmp_1 = entity.getToggleX() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        final int _tmp_2 = entity.getToggleY() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        final int _tmp_3 = entity.getToggleZ() ? 1 : 0;
        statement.bindLong(9, _tmp_3);
        final int _tmp_4 = entity.getToggleW() ? 1 : 0;
        statement.bindLong(10, _tmp_4);
        if (entity.getSleepMinutes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getSleepMinutes());
        }
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM entries";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM entries WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final EntryEntity entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfEntryEntity.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<EntryEntity> entries,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEntryEntity.insert(entries);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<EntryEntity>> observeAll() {
    final String _sql = "SELECT * FROM entries ORDER BY createdAt DESC, id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"entries"}, new Callable<List<EntryEntity>>() {
      @Override
      @NonNull
      public List<EntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfMoodRating = CursorUtil.getColumnIndexOrThrow(_cursor, "moodRating");
          final int _cursorIndexOfMoodEmojisCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "moodEmojisCsv");
          final int _cursorIndexOfToggleX = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleX");
          final int _cursorIndexOfToggleY = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleY");
          final int _cursorIndexOfToggleZ = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleZ");
          final int _cursorIndexOfToggleW = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleW");
          final int _cursorIndexOfSleepMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepMinutes");
          final List<EntryEntity> _result = new ArrayList<EntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EntryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final Integer _tmpMoodRating;
            if (_cursor.isNull(_cursorIndexOfMoodRating)) {
              _tmpMoodRating = null;
            } else {
              _tmpMoodRating = _cursor.getInt(_cursorIndexOfMoodRating);
            }
            final String _tmpMoodEmojisCsv;
            _tmpMoodEmojisCsv = _cursor.getString(_cursorIndexOfMoodEmojisCsv);
            final boolean _tmpToggleX;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfToggleX);
            _tmpToggleX = _tmp_2 != 0;
            final boolean _tmpToggleY;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfToggleY);
            _tmpToggleY = _tmp_3 != 0;
            final boolean _tmpToggleZ;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfToggleZ);
            _tmpToggleZ = _tmp_4 != 0;
            final boolean _tmpToggleW;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfToggleW);
            _tmpToggleW = _tmp_5 != 0;
            final Integer _tmpSleepMinutes;
            if (_cursor.isNull(_cursorIndexOfSleepMinutes)) {
              _tmpSleepMinutes = null;
            } else {
              _tmpSleepMinutes = _cursor.getInt(_cursorIndexOfSleepMinutes);
            }
            _item = new EntryEntity(_tmpId,_tmpCreatedAt,_tmpTitle,_tmpBody,_tmpMoodRating,_tmpMoodEmojisCsv,_tmpToggleX,_tmpToggleY,_tmpToggleZ,_tmpToggleW,_tmpSleepMinutes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllOnce(final Continuation<? super List<EntryEntity>> $completion) {
    final String _sql = "SELECT * FROM entries ORDER BY createdAt DESC, id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EntryEntity>>() {
      @Override
      @NonNull
      public List<EntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfMoodRating = CursorUtil.getColumnIndexOrThrow(_cursor, "moodRating");
          final int _cursorIndexOfMoodEmojisCsv = CursorUtil.getColumnIndexOrThrow(_cursor, "moodEmojisCsv");
          final int _cursorIndexOfToggleX = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleX");
          final int _cursorIndexOfToggleY = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleY");
          final int _cursorIndexOfToggleZ = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleZ");
          final int _cursorIndexOfToggleW = CursorUtil.getColumnIndexOrThrow(_cursor, "toggleW");
          final int _cursorIndexOfSleepMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepMinutes");
          final List<EntryEntity> _result = new ArrayList<EntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EntryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.toInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final Integer _tmpMoodRating;
            if (_cursor.isNull(_cursorIndexOfMoodRating)) {
              _tmpMoodRating = null;
            } else {
              _tmpMoodRating = _cursor.getInt(_cursorIndexOfMoodRating);
            }
            final String _tmpMoodEmojisCsv;
            _tmpMoodEmojisCsv = _cursor.getString(_cursorIndexOfMoodEmojisCsv);
            final boolean _tmpToggleX;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfToggleX);
            _tmpToggleX = _tmp_2 != 0;
            final boolean _tmpToggleY;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfToggleY);
            _tmpToggleY = _tmp_3 != 0;
            final boolean _tmpToggleZ;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfToggleZ);
            _tmpToggleZ = _tmp_4 != 0;
            final boolean _tmpToggleW;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfToggleW);
            _tmpToggleW = _tmp_5 != 0;
            final Integer _tmpSleepMinutes;
            if (_cursor.isNull(_cursorIndexOfSleepMinutes)) {
              _tmpSleepMinutes = null;
            } else {
              _tmpSleepMinutes = _cursor.getInt(_cursorIndexOfSleepMinutes);
            }
            _item = new EntryEntity(_tmpId,_tmpCreatedAt,_tmpTitle,_tmpBody,_tmpMoodRating,_tmpMoodEmojisCsv,_tmpToggleX,_tmpToggleY,_tmpToggleZ,_tmpToggleW,_tmpSleepMinutes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

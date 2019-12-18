package com.dimowner.audiorecorder.app.trash;

import com.dimowner.audiorecorder.AppConstants;
import com.dimowner.audiorecorder.BackgroundQueue;
import com.dimowner.audiorecorder.R;
import com.dimowner.audiorecorder.app.lostrecords.RecordItem;
import com.dimowner.audiorecorder.data.FileRepository;
import com.dimowner.audiorecorder.data.Prefs;
import com.dimowner.audiorecorder.data.database.LocalRepository;
import com.dimowner.audiorecorder.data.database.Record;
import com.dimowner.audiorecorder.util.AndroidUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 15.12.2019.
 * @author Dimowner
 */
public class TrashPresenter implements TrashContract.UserActionsListener {

	private TrashContract.View view;
	private final BackgroundQueue loadingTasks;
	private final BackgroundQueue recordingsTasks;
	private final FileRepository fileRepository;
	private final LocalRepository localRepository;
	private final Prefs prefs;

	public TrashPresenter(BackgroundQueue loadingTasks, BackgroundQueue recordingsTasks,
								 FileRepository fileRepository, LocalRepository localRepository, Prefs prefs) {
		this.loadingTasks = loadingTasks;
		this.recordingsTasks = recordingsTasks;
		this.fileRepository = fileRepository;
		this.localRepository = localRepository;
		this.prefs = prefs;
	}

	@Override
	public void bindView(final TrashContract.View v) {
		this.view = v;

		loadingTasks.postRunnable(new Runnable() {
			@Override public void run() {
				List<Record> records = localRepository.getTrashRecords();
				final ArrayList<RecordItem> list = new ArrayList<>();
				for (Record r : records) {
					list.add(new RecordItem(r.getId(), r.getName(), r.getDuration(), r.getPath()));
				}
				AndroidUtils.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (list.isEmpty()) {
							view.showEmpty();
						} else {
							view.showRecords(list);
							view.hideEmpty();
						}
					}
				});
			}
		});
	}

	@Override
	public void unbindView() {
		this.view = null;
	}

	@Override
	public void clear() {
		unbindView();
	}

	@Override
	public void onRecordInfo(String name, long duration, String location) {
		String format;
		if (location.contains(AppConstants.M4A_EXTENSION)) {
			format = AppConstants.M4A_EXTENSION;
		} else if (location.contains(AppConstants.WAV_EXTENSION)) {
			format = AppConstants.WAV_EXTENSION;
		} else {
			format = "";
		}
		view.showRecordInfo(name, format, duration, new File(location).length(), location);
	}

	@Override
	public void deleteRecordFromTrash(final int id, final String path) {
		recordingsTasks.postRunnable(new Runnable() {
			@Override
			public void run() {
				localRepository.removeFromTrash(id);
				fileRepository.deleteRecordFile(path);
				AndroidUtils.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (view != null) {
							view.showMessage(R.string.record_deleted_successfully);
							view.recordDeleted(id);
						}
					}
				});
			}
		});
	}

	@Override
	public void restoreRecordFromTrash(final int id) {
		recordingsTasks.postRunnable(new Runnable() {
			@Override
			public void run() {
				localRepository.restoreFromTrash(id);
				AndroidUtils.runOnUIThread(new Runnable() {
					@Override
					public void run() {
						if (view != null) {
							view.showMessage(R.string.record_restored_successfully);
							view.recordRestored(id);
						}
					}
				});
			}
		});
	}
}

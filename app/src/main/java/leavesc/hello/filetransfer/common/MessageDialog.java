package leavesc.hello.filetransfer.common;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;

/**
 * 作者：叶应是叶
 * 时间：2018/2/15 11:37
 * 描述：
 */
public class MessageDialog extends DialogFragment {

    private DialogInterface.OnClickListener positiveCallback;

    private DialogInterface.OnClickListener negativeCallback;

    private String title;

    private String message;

    private boolean cancelable;

    private String positiveText;

    private String negativeText;

    public void show(String title, String message, boolean cancelable, DialogInterface.OnClickListener positiveCallback,
                     DialogInterface.OnClickListener negativeCallback, FragmentManager fragmentManager) {
        this.title = title;
        this.message = message;
        this.cancelable = cancelable;
        this.positiveCallback = positiveCallback;
        this.negativeCallback = negativeCallback;
        this.positiveText = "确认";
        this.negativeText = "取消";
        show(fragmentManager, "MessageDialog");
    }

    public void show(@StringRes int titleRes, @StringRes int messageRes, boolean cancelable, DialogInterface.OnClickListener positiveCallback,
                     DialogInterface.OnClickListener negativeCallback, FragmentManager fragmentManager) {
        this.show(getString(titleRes), getString(messageRes), cancelable, positiveCallback, negativeCallback, fragmentManager);
    }

    public void show(@StringRes int titleRes, @StringRes int messageRes, DialogInterface.OnClickListener positiveCallback, FragmentManager fragmentManager) {
        this.show(getString(titleRes), getString(messageRes), true, positiveCallback, null, fragmentManager);
    }

    public void show(String title, String message, DialogInterface.OnClickListener positiveCallback, FragmentManager fragmentManager) {
        this.show(title, message, true, positiveCallback, null, fragmentManager);
    }

    public void showConfirm(String title, String message, DialogInterface.OnClickListener positiveCallback, FragmentManager fragmentManager) {
        this.title = title;
        this.message = message;
        this.positiveCallback = positiveCallback;
        this.cancelable = false;
        this.positiveText = "确认";
        show(fragmentManager, "MessageDialog");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        builder.setCancelable(cancelable);
        if (positiveText != null) {
            builder.setPositiveButton("确定", positiveCallback);
        }
        if (negativeText != null) {
            builder.setNegativeButton("取消", negativeCallback);
        }
        return builder.create();
    }

}

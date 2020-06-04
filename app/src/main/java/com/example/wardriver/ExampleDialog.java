package com.example.wardriver;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


public class ExampleDialog extends AppCompatDialogFragment {
    private EditText nameInput;
    private ExampleDialogListener listener;
    String dialogTitle;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflator = getActivity().getLayoutInflater();
        View view = inflator.inflate(R.layout.layout_dialog, null);

        builder.setView(view)
                .setTitle(dialogTitle)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.startScan();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInput.getText().toString();
                        listener.setFileName(name, dialogTitle);
                        listener.startScan();

                    }
                });
        nameInput = view.findViewById(R.id.fileName);
        return builder.create();


    }

    //Attach this listener from MainActivity
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (ExampleDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement ExampleDialogListener");
        }
    }

    //Implemented by MainActivity so that it can listen event from this dialog
    public interface ExampleDialogListener{
        void setFileName(String name, String title);
        void startScan();

    }


}

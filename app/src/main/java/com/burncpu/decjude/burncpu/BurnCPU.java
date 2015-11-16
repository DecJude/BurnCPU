package com.burncpu.decjude.burncpu;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.burncpu.decjude.burncpu.ShellUtils.CommandResult;

public class BurnCPU extends ActionBarActivity {
    private TextView mPsBurn;
    private Button[] btnNum =  new Button[9];
    private int burnCortexA9Pid[] = new int[8];
    private int btnNumOfBurnCPUs = 0;
    private int currentNumOfBurnCPUs = 0;
    GetPidThread gpt = new GetPidThread();
    Thread getPid =  new Thread(gpt);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_burn_cpu);

        findViews();
        setListeners();

        CopyThread tt = new CopyThread();
        new Thread(tt).start();

    }



    private void findViews() {
        btnNum[0] = (Button)findViewById(R.id.btn0);
        btnNum[1] = (Button)findViewById(R.id.btn1);
        btnNum[2] = (Button)findViewById(R.id.btn2);
        btnNum[3] = (Button)findViewById(R.id.btn3);
        btnNum[4] = (Button)findViewById(R.id.btn4);
        btnNum[5] = (Button)findViewById(R.id.btn5);
        btnNum[6] = (Button)findViewById(R.id.btn6);
        btnNum[7] = (Button)findViewById(R.id.btn7);
        btnNum[8] = (Button)findViewById(R.id.btn8);

        mPsBurn = (TextView)findViewById(R.id.psBurn);
    }

    private void setListeners() {
        NumberListener nl = new NumberListener();

        for (int i = 0; i < btnNum.length; i++) {
            btnNum[i].setOnClickListener(nl);
        }
    }

    private class NumberListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Button btn = (Button)v;
            String input = btn.getText().toString();

            getRunningProcessNum();

            btnNumOfBurnCPUs = Integer.parseInt(input);

            if (btnNumOfBurnCPUs > currentNumOfBurnCPUs) {
                startBurnCPUProcess(btnNumOfBurnCPUs - currentNumOfBurnCPUs);
            } else if (btnNumOfBurnCPUs < currentNumOfBurnCPUs) {
                killBurnCPUProcess(currentNumOfBurnCPUs - btnNumOfBurnCPUs);
            } else {
                // do nothing
            }
            getRunningProcessNum();
            mPsBurn.setText(Integer.toString(currentNumOfBurnCPUs));

        }
    }

    public class CopyThread implements Runnable {

        @Override
        public void run() {
            try {
                copyBigDataToSD("burnCortexA9", "/data/burnCortexA9");
            } catch (IOException e) {
                e.printStackTrace();
            }
            ShellUtils.execCommand("chmod 777 /data/burnCortexA9", false);
        }
    }

    public class GetPidThread implements  Runnable {

        @Override
        public void run() {

            ShellUtils.execCommand("ps | grep burnCortexA9", false);
        }
    }

    private void getRunningProcessNum() {
        CommandResult commandResult = ShellUtils.execCommand("ps burn", false);
        String lines[] = commandResult.successMsg.toString().split("\n");
        int lineCount = 0;
        int psCount = 0;
        currentNumOfBurnCPUs = 0;
        for(String line : lines) {

            if (lineCount > 0) {
                burnCortexA9Pid[psCount] = Integer.parseInt(line.substring(9,15).trim());
                psCount ++;
            }
            lineCount ++;
        }
        currentNumOfBurnCPUs = psCount;
        mPsBurn.setText(Integer.toString(currentNumOfBurnCPUs));
    }

    private void killBurnCPUProcess(int num) {

        for (int i = 0; i < num; i++) {
            ShellUtils.execCommand("kill -9 " + burnCortexA9Pid[i], false);
        }
    }

    private void killAllBurnCPUProcess() {
        getRunningProcessNum();

        for (int i = 0; i < currentNumOfBurnCPUs; i++) {
            ShellUtils.execCommand("kill -9 " + burnCortexA9Pid[i], false);
        }
    }

    private void startBurnCPUProcess(int num) {

        for (int i = 0; i < num; i++ ) {
            Thread thread = new Thread(rStartBurnCPU);
            thread.start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    Runnable rStartBurnCPU =  new Runnable() {

        @Override
        public void run() {
            ShellUtils.execCommand("./data/burnCortexA9", false);
        }
    };


    private void copyBigDataToSD(String srcFile, String destFile) throws IOException {
        InputStream input;
        OutputStream output = new FileOutputStream(destFile);
        input = this.getAssets().open(srcFile);

        byte[] buffer = new byte[1024];
        int length = input.read(buffer);
        while (length > 0) {
            output.write(buffer, 0, length);
            length = input.read(buffer);
        }

        output.flush();
        input.close();
        output.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getRunningProcessNum();
    }

    @Override
    protected void onStop() {
        super.onDestroy();
        killAllBurnCPUProcess();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        killAllBurnCPUProcess();
    }

}

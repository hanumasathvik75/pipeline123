package com.ignitionai.nativeapp;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.ignitionai.application.*;
import com.ignitionai.obdgenerator.config.*;
import com.ignitionai.obdgenerator.generator.*;
import com.ignitionai.phase6.*;
import com.ignitionai.phase7.latex.LatexGenerator;
import com.ignitionai.phase7.validation.CertificateValidator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Native Java Android widgets; all calculations run in the shared core. */
public final class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView status, assessment;
    private EditText vehicle, scenario;
    private InspectionResult result;
    private final List<Button> busyButtons = new ArrayList<>();
    private boolean destroyed;
    private final BroadcastReceiver captureReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {
            status.setText(intent.getStringExtra("status"));
            if (intent.getBooleanExtra("complete", false)) {
                String file = intent.getStringExtra("file");
                if (file != null) load(new File(getFilesDir(), "sessions/" + file));
            }
        }
    };
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,32,20,16);
        TextView title = new TextView(this); title.setText("IgnitionAI"); title.setTextSize(25); root.addView(title);
        status = new TextView(this); status.setText("Local vehicle assessment • Preliminary model"); root.addView(status);
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); scroll.addView(content);
        vehicle = new EditText(this); vehicle.setSingleLine(true); vehicle.setHint("Vehicle reference / registration"); vehicle.setText(state == null ? "NEXON-TEST" : state.getString("vehicle")); content.addView(vehicle);
        button(content, "Connect paired Bluetooth dongle", this::connect);
        Button stop = new Button(this); stop.setText("Stop capture"); stop.setOnClickListener(v -> { stopService(new Intent(this, CaptureService.class)); status.setText("Capture stopped. Open the latest session in History."); }); content.addView(stop);
        button(content, "Open OBD session", () -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,10); });
        button(content, "History", this::history);
        TextView simTitle = new TextView(this); simTitle.setText("Configurable simulator"); simTitle.setTextSize(20); content.addView(simTitle);
        scenario = new EditText(this); scenario.setTextSize(12); scenario.setTypeface(Typeface.MONOSPACE); scenario.setGravity(Gravity.TOP); scenario.setMinLines(5); scenario.setMaxLines(7); scenario.setHorizontallyScrolling(true);
        scenario.setText(state == null ? ScenarioFile.example() : state.getString("scenario")); content.addView(scenario);
        Button generate = button(content,"Generate and assess", () -> {
            String configuration = scenario.getText().toString();
            work(() -> {
                result = service().assess(new ObdGenerator(ScenarioFile.parse(configuration)).generate().getPublicStream());
                saveResult(); return report();
            });
        }); generate.setId(101);
        assessment = new TextView(this); assessment.setId(102); assessment.setTextIsSelectable(true); assessment.setTextSize(15); assessment.setText("No assessment yet."); content.addView(assessment);
        button(content,"Export session for Windows", () -> export(11,"application/x-ndjson","session.jsonl"));
        button(content,"Export LaTeX certificate", () -> export(12,"application/x-tex","certificate.tex"));
        button(content,"Export PDF report", () -> export(13,"application/pdf","assessment.pdf"));
        setContentView(root);
        IntentFilter filter = new IntentFilter(CaptureService.STATUS);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(captureReceiver, filter, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(captureReceiver,filter);
        work(() -> { installSensors(); return "Generate a simulation, import a session or connect a paired Classic Bluetooth ELM327 dongle."; });
    }
    private Button button(LinearLayout parent, String text, Runnable action) {
        Button b=new Button(this); b.setText(text); b.setOnClickListener(v -> action.run()); parent.addView(b); busyButtons.add(b); return b;
    }
    private interface Job { String run() throws Exception; }
    private void work(Job job) {
        busyButtons.forEach(b -> b.setEnabled(false)); status.setText("Working…");
        executor.execute(() -> {
            try { String text=job.run(); runOnUiThread(() -> { if (!destroyed) { assessment.setText(text); status.setText("Ready • Stored on this device"); busyButtons.forEach(b -> b.setEnabled(true)); } }); }
            catch (Exception e) { runOnUiThread(() -> { if (!destroyed) { status.setText("Unable to finish: " + e.getMessage()); busyButtons.forEach(b -> b.setEnabled(true)); } }); }
        });
    }
    private InspectionService service() { return new InspectionService(new File(getFilesDir(),"sensors")); }
    private void installSensors() throws IOException {
        File dir=new File(getFilesDir(),"sensors");dir.mkdirs();
        for(String name: getAssets().list("sensors")) try(InputStream in=getAssets().open("sensors/"+name)) { Files.copy(in,new File(dir,name).toPath(),StandardCopyOption.REPLACE_EXISTING); }
    }
    private void saveResult() throws IOException {
        File dir=new File(getFilesDir(),"sessions");dir.mkdirs();
        SessionFiles.write(new File(dir,result.certificate.getCertificateId()+".jsonl").toPath(), result.messages);
    }
    private void load(File file) { work(() -> { result=service().assess(SessionFiles.read(file.toPath())); return report(); }); }
    private String report() {
        if(result==null)return "No assessment";
        VehicleHealthAssessment a=result.certificate.getHealthAssessment();
        StringBuilder b=new StringBuilder("Vehicle: "+a.getVehicleId()+"\n"+a.getAssessmentTimestamp()+"\nRecords: "+result.messages.size()+"\nVHI: "+value(a.getVehicleHealthIndex())+" ("+a.getHealthBand()+")\nData: "+a.getDataQualityStatus()+"\nCoverage: "+value(a.getSubsystemCoverage())+"\n");
        for(SubsystemHealthAssessment s:a.getSubsystemAssessments())b.append("\n").append(s.getSubsystemName()).append(": ").append(value(s.getScore())).append(" | ").append(s.getSeverity()).append("\n").append(String.join(", ",s.getDegradedDataFlags())).append("\n");
        b.append("\nEpisodes: ").append(result.episodes.size()).append("\n").append(String.join("\n",result.notes));
        result.degradation.forEach(d->b.append("\n").append(d.getSubsystemId()).append(": ").append(d.getDegradationState()).append("; risk ").append(d.getRiskStatus()));
        result.episodes.forEach(ep->b.append("\nEvidence ").append(ep.getEpisodeId()).append(": ").append(String.join(", ",ep.getEvidenceReferences())));
        return b.toString();
    }
    private static String value(Double n){return n==null?"Unavailable":String.format(Locale.ROOT,"%.2f",n);}
    @android.annotation.SuppressLint("MissingPermission")
    private void connect() {
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},20);return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},21);}
        BluetoothAdapter adapter=getSystemService(BluetoothManager.class).getAdapter();
        if(adapter==null){status.setText("This device has no Bluetooth adapter");return;}
        if(!adapter.isEnabled()){startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));return;}
        List<BluetoothDevice> devices=new ArrayList<>(adapter.getBondedDevices());
        if(devices.isEmpty()){status.setText("Pair your ELM327 dongle in Android Bluetooth settings first");return;}
        String[] names=new String[devices.size()];for(int i=0;i<names.length;i++)names[i]=devices.get(i).getName()+" ("+devices.get(i).getAddress()+")";
        new AlertDialog.Builder(this).setTitle("Select paired dongle").setItems(names,(d,i)->{
            String id=vehicle.getText().toString().trim();if(id.isEmpty()){status.setText("Enter a vehicle reference");return;}
            startForegroundService(new Intent(this,CaptureService.class).putExtra("address",devices.get(i).getAddress()).putExtra("vehicle",id));
        }).show();
    }
    private void history(){
        File dir=new File(getFilesDir(),"sessions");File[] files=dir.listFiles((d,n)->n.endsWith(".jsonl")&&!n.endsWith(".raw.jsonl"));
        if(files==null||files.length==0){status.setText("No saved sessions");return;}
        Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());String[] names=Arrays.stream(files).map(File::getName).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle("Saved sessions").setItems(names,(d,i)->load(files[i])).show();
    }
    private void export(int request,String type,String name){if(result==null){status.setText("Run an assessment first");return;}startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(type).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,name),request);}
    protected void onActivityResult(int request,int code,Intent data){
        super.onActivityResult(request,code,data);if(code!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        work(()->{
            if(request==10){
                File temp=File.createTempFile("import-",".jsonl",getCacheDir());
                try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
                    byte[] buffer=new byte[8192];long total=0;int n;while((n=in.read(buffer))!=-1){total+=n;if(total>64L*1024*1024)throw new IOException("Import exceeds 64 MB");out.write(buffer,0,n);}
                }
                try{result=service().assess(SessionFiles.read(temp.toPath()));saveResult();}finally{temp.delete();}
            }else{
                if(request!=11){var validation=new CertificateValidator().validateSnapshot(result.certificate);if(!validation.isValid())throw new IOException(validation.getErrors().toString());}
                try(OutputStream out=getContentResolver().openOutputStream(uri)){
                    if(request==11){for(var m:result.messages)out.write((com.ignitionai.obdinput.storage.ObdJsonMapper.serialize(m)+"\n").getBytes(StandardCharsets.UTF_8));}
                    else if(request==12)out.write(new LatexGenerator().generateCertificate(result.certificate).getBytes(StandardCharsets.UTF_8));
                    else if(request==13)writePdf(out);
                }
            }
            return report();
        });
    }
    private void writePdf(OutputStream output)throws IOException{
        PdfDocument doc=new PdfDocument();
        try {
            Paint paint=new Paint();paint.setTextSize(11);int pageNo=1,y=45;PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());
            for(String line:("IgnitionAI assessment\n"+result.certificate.getCertificateId()+"\n"+report()).split("\n")){
                do{int count=Math.min(85,line.length());String text=line.substring(0,count);line=line.substring(count);
                    if(y>800){doc.finishPage(page);page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,++pageNo).create());y=45;}
                    page.getCanvas().drawText(text,30,y,paint);y+=16;
                }while(!line.isEmpty());
            }
            doc.finishPage(page);doc.writeTo(output);
        } finally { doc.close(); }
    }
    protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("vehicle",vehicle.getText().toString());state.putString("scenario",scenario.getText().toString());}
    protected void onDestroy(){destroyed=true;unregisterReceiver(captureReceiver);executor.shutdownNow();super.onDestroy();}
}

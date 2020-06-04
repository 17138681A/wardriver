package com.example.wardriver;

        import java.util.List;
        import android.content.Context;
        import android.net.wifi.ScanResult;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.BaseAdapter;
        import android.widget.TextView;

public class ListAdapter extends BaseAdapter {

    Context context;
    LayoutInflater inflater;
    List<ScanResult> wifiList;
    String location;
    String date;
    String dataRow;
    String tempData = "";


    public ListAdapter(Context context, List wifiList, String l, String d) {

        this.context = context;
        this.wifiList = wifiList;
        this.location = l;
        this.date = d;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return wifiList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        System.out.println("viewpos" + position);
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.dataset, null);
            holder = new Holder();
            holder.tvDetails = (TextView) view.findViewById(R.id.tvDetails);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        //When no location received
        if(location == null)
            location = "\nScanning location...";

        //Create a row of data
        dataRow = "SSID :: " + wifiList.get(position).SSID
                + "\nBSSID :: " + wifiList.get(position).BSSID
                +  location
                + "\nDate :: " + date;

        //Temporary memorize current data
        tempData += dataRow + "\n\n";

        holder.tvDetails.setText(dataRow);

        return view;
    }

    class Holder {
        TextView tvDetails;
    }

    public String getTempData(){
        return tempData;
    }

    public boolean locationIsScanning(){
        if(location == "\nScanning location...")
            return true;
        else
            return false;
    }


}

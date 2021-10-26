import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONObject;

public class validation {
    public static void main(String[] args){
        String trainingFile = "nursery.csv";
        int n = 20;
        String restrictionsFile = "nurseryRestrictions.txt";
        validate(trainingFile, n, restrictionsFile);
    }

    public static void validate(String training, int n, String restrictions){
        HashMap<Integer, List<ArrayList<String>>> folds = new HashMap<>();
        HashMap<Integer, List<ArrayList<String>>> trainingSets = new HashMap<>();
        List<ArrayList<String>> D = getData(training);
        ArrayList<String> firstLine = D.get(0);
        ArrayList<String> secondLine = D.get(1);
        ArrayList<String> thirdLine = D.get(2);
        D.remove(0);
        D.remove(0);
        D.remove(0);
        for(int k=1; k<=n; k++){
            List<ArrayList<String>> newList= new ArrayList<>();
            List<ArrayList<String>> newTrainingSet= new ArrayList<>();
            newList.add(firstLine);
            newList.add(secondLine);
            newList.add(thirdLine);
            newTrainingSet.add(firstLine);
            newTrainingSet.add(secondLine);
            newTrainingSet.add(thirdLine);
            int holdoutSetStartIdx = (k-1) * (D.size()/n);
            int holdoutSetEndIdx = (k * D.size()/n) - (k - 1);
            List<ArrayList<String>> Dj = D.subList((k-1) * (D.size()/n), (k * D.size()/n) - (k - 1));
            for (ArrayList<String> newItem : Dj){
                newList.add(newItem);
            }
            for(int i=0; i<D.size(); i++){
                if(i <= holdoutSetEndIdx && i >= holdoutSetStartIdx){
                    continue;
                } else{
                    newTrainingSet.add(D.get(i));
                }
            }
            trainingSets.put(k, newTrainingSet);
            folds.put(k, newList);
        }
        for(int i =1 ; i<=D.size() % n; i++){
            Integer targIdx = D.size() - i - 1;
            List<ArrayList<String>> currD = new ArrayList<>();
            for(ArrayList<String> item : folds.get(i)){
                currD.add(item);
            }
            currD.add(D.get(targIdx));
            folds.put(i, currD);
        }
        for(Map.Entry<Integer, List<ArrayList<String>>> set : folds.entrySet()){
            JSONObject c45 = InduceC45.run(set.getValue(), training, restrictions);
            // System.out.println("C45 done");
            ArrayList<String> classifications = classify.run(c45, trainingSets.get(set.getKey()));
            // System.out.println(classifications.toString());
        }


        // JSONObject c45 = InduceC45.run(D, "agaricus-lepiota.csv", "restrictions.txt");
        // ArrayList<String> records = new ArrayList<>();

    }

    public static List<ArrayList<String>> getData(String path){
        Scanner sc;
        List<ArrayList<String>> data = new ArrayList<>();
        try {
            sc = new Scanner(new File(path));
            while (sc.hasNextLine()){
                ArrayList<String> lineVals = new ArrayList<>();
                String[] line = sc.nextLine().split(",");
                for(String s : line){
                    lineVals.add(s);
                }
                data.add(lineVals);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return data;
    }
}

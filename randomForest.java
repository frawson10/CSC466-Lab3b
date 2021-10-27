import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONObject;

import java.lang.Math;  

public class randomForest {
    public static void main(String[] args){
        int numAttributes = 2;
        int numDataPoints = 20;
        int numTrees = 25;
        String targFile = "numeric/iris.data.csv";
        List<ArrayList<String>> D = getData(targFile);
        ArrayList<ArrayList<String>> A = new ArrayList<>();
        A.add(D.get(0));
        A.add(D.get(1));
        String classVar = D.get(2).get(0);
        ArrayList<String> firstLine = D.get(0);
        ArrayList<String> secondLine = D.get(1);
        ArrayList<String> thirdLine = D.get(2);
        D.remove(0);
        D.remove(0);
        D.remove(0);
        int classVarLoc = -1;
        for(int i=0; i<A.get(0).size(); i++){
            if(classVar.equals(A.get(0).get(i))){
                classVarLoc = i;
                break;
            }
        }
        HashMap<Integer, List<ArrayList<String>>> folds = new HashMap<>();
        HashMap<Integer, List<ArrayList<String>>> trainingSets = new HashMap<>();
        for(int k=1; k<=10; k++){
            List<ArrayList<String>> newList= new ArrayList<>();
            List<ArrayList<String>> newTrainingSet= new ArrayList<>();
            newList.add(firstLine);
            newList.add(secondLine);
            newList.add(thirdLine);
            newTrainingSet.add(firstLine);
            newTrainingSet.add(secondLine);
            newTrainingSet.add(thirdLine);
            int holdoutSetStartIdx = (k-1) * (D.size()/10);
            int holdoutSetEndIdx = (k * D.size()/10) - (k - 1);
            List<ArrayList<String>> Dj = D.subList((k-1) * (D.size()/10), (k * D.size()/10) - (k - 1));
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
        for(int i =1 ; i<=D.size() % 10; i++){
            Integer targIdx = D.size() - i - 1;
            List<ArrayList<String>> currD = folds.get(i);
            /* for(ArrayList<String> item : folds.get(i)){
                currD.add(item);
            }
            currD.add(D.get(targIdx));
            folds.put(i, currD); */
            currD.add(D.get(targIdx));
        }
        for(Map.Entry<Integer, List<ArrayList<String>>> set : trainingSets.entrySet()){
            ArrayList<JSONObject> forest = new ArrayList<>();
            for(int n=0; n<numTrees; n++){
                List<ArrayList<String>> train = new ArrayList<>();
                HashMap<Integer, Boolean> randIdxs = new HashMap<>();
                for(int i=0; i<numDataPoints; i++){
                    int rand = -1;
                    while(rand<3){
                        rand = (int)(Math.random()*(set.getValue().size()));
                    }
                    randIdxs.put(rand, true);
                }
                for(int i=0; i<set.getValue().size(); i++){
                    if(i==0 || i==1 || i==2 || randIdxs.get(i) != null){
                        train.add(set.getValue().get(i));
                    }
                }
                // System.out.println(train);
                ArrayList<String> restrictions = buildRestrictions(numAttributes, classVarLoc, A);
                forest.add(InduceC45.run(train, targFile, restrictions));
            }
            // System.out.println(forest.size());
            // JSONObject c45 = InduceC45.run(set.getValue(), training, restrictions);
            // System.out.println("C45 done");
            // System.out.println(c45.toString(4));
            // ArrayList<String> classifications = classify.run(c45, trainingSets.get(set.getKey()));
            // System.out.println(classifications.toString());
            for(int i=0; i<forest.size(); i++){
                System.out.println("HELLO");
                System.out.println(i);
                System.out.println(folds.get(set.getKey()));
                ArrayList<String> classifications = classify.run(forest.get(i), folds.get(set.getKey()));
            }
        }

    }

    public static ArrayList<String> buildRestrictions(int numAttributes, int classVarLoc, ArrayList<ArrayList<String>> A){
        ArrayList<String> restrictions = new ArrayList<>();
        HashMap<Integer, Boolean> chosenAtts = new HashMap<>();
        for(int i=0; i<numAttributes; i++){
            while(true){
                int randomAtt = (int)(Math.random()*(A.get(0).size()));
                if(randomAtt != classVarLoc && chosenAtts.get(randomAtt) == null){
                    chosenAtts.put(randomAtt, true);
                    break;
                }
            }
        }
        for(int i=0; i<A.get(0).size(); i++){
            if(chosenAtts.get(i) == null && i != classVarLoc){
                restrictions.add("0");
            }else{
                restrictions.add("1");
            }
        }
        return restrictions;
    }

    public static ArrayList<ArrayList<String>> getData(String path){
        Scanner sc;
        ArrayList<ArrayList<String>> data = new ArrayList<>();
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

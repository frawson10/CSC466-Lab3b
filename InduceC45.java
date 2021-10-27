import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class InduceC45{
    public static void main(String[] args){
        ArrayList<ArrayList<String>> D = getData("numeric/letter-recognition.data.csv");
        ArrayList<ArrayList<String>> A = new ArrayList<>();
        ArrayList<String> restrictions = readRestrictions("numeric/letter-recognition-restrictions.txt");
        A.add(D.get(0));
        A.add(D.get(1));
        A.add(restrictions);
        String classVar = D.get(2).get(0);
        D.remove(0);
        D.remove(0);
        D.remove(0);
        double threshold = 0.37;
        Node tree = c45(D, A, threshold, classVar);
        printJSON(tree, "numeric/letter-recognition.data.csv");

    }

    public static JSONObject run(List<ArrayList<String>> D, String training, ArrayList<String> restrictions){
        ArrayList<ArrayList<String>> A = new ArrayList<>();
        // ArrayList<String> restrictions = readRestrictions(restrictionsFile);
        A.add(D.get(0));
        A.add(D.get(1));
        A.add(restrictions);
        String classVar = D.get(2).get(0);
        D.remove(0);
        D.remove(0);
        D.remove(0);
        double threshold = 0.15;
        Node tree = c45(D, A, threshold, classVar);
        JSONObject json = createJSON(tree, training);
        // printJSON(tree, training);
        return json;
    }

    public static Node c45(List<ArrayList<String>> D, 
        ArrayList<ArrayList<String>> A, double threshold, String classVar){
        int classVarLoc = -1;
        for(int i = 0; i<A.get(0).size(); i++){
            if(A.get(0).get(i).equals(classVar)){
                classVarLoc = i;
            }
        }
        // if purity then return leaf with class var
        boolean purityFlag = true;
        String tempClassVar = D.get(0).get(classVarLoc);
        for(ArrayList<String> point : D){
            if(point.get(classVarLoc) != tempClassVar){
                purityFlag = false;
                break;
            }
        }
        if(purityFlag){
            return new Node("", tempClassVar, null, 1);
        }
        // if no more categorical attributes other than class var, choose most frequent label
        int numCatVarLeft = 0;
        for(int i=0; i<A.get(2).size(); i++){
            if(A.get(2).get(i).equals("0") && !A.get(1).get(i).equals("0")){
                numCatVarLeft += 1;
            }
        }
        if(numCatVarLeft != 0){
            Pair winner = popularityContest(D, A, classVarLoc);
            return new Node("", winner.att, null, winner.popularity/Double.valueOf(D.size()));
        }
        // try split
        else{
            SplittingAttPair splittingAtt = selectSplittingAttribute(D, A, threshold, classVarLoc);
            if(splittingAtt == null){
                Pair winner = popularityContest(D, A, classVarLoc);
                return new Node("", winner.att, null, winner.popularity/Double.valueOf(D.size()));
            } else {
                //stuff after split
                Node tree = new Node(A.get(0).get(splittingAtt.attIdx), "", new ArrayList<>(), -1);
                HashMap<String, ArrayList<ArrayList<String>>> splits = new HashMap<>();
                if(Integer.valueOf(A.get(1).get(splittingAtt.attIdx)) != 0){ //categorical
                    for(ArrayList<String> point : D){
                        if(splits.get(point.get(splittingAtt.attIdx)) == null){
                            ArrayList<ArrayList<String>> temp = new ArrayList<>();
                            splits.put(point.get(splittingAtt.attIdx), temp);
                        }
                        ArrayList<ArrayList<String>> temp = splits.get(point.get(splittingAtt.attIdx));
                        temp.add(point);
                        splits.put(point.get(splittingAtt.attIdx), temp);
                    }
                    for(Map.Entry<String, ArrayList<ArrayList<String>>> set : splits.entrySet()){
                        ArrayList<ArrayList<String>> newA = new ArrayList<>();
                        for(ArrayList<String> a : A){
                            newA.add(a);
                        }
                        newA.get(2).set(splittingAtt.attIdx, "0");
                        Node subTree = c45(set.getValue(), newA, threshold, classVar);
                        tree.addEdge(set.getKey(), subTree, "");
                    }
                    if(tree.edges.size() < Integer.valueOf(A.get(1).get(splittingAtt.attIdx))){
                        for(int i=0; i<D.size(); i++){
                            if(splits.get(D.get(i).get(splittingAtt.attIdx)) != null){
                                Pair winner = popularityContest(D, A, classVarLoc);
                                Node n = new Node("", winner.att, null, winner.popularity/Double.valueOf(D.size()));
                                tree.addEdge("ghost", n, "");
                            }
                        }
                    }
                } else { //numeric
                    List<ArrayList<String>> lessOrEqual = new ArrayList<>();
                    List<ArrayList<String>> greater = new ArrayList<>();
                    for(ArrayList<String> item : D){
                        if(Double.valueOf(item.get(splittingAtt.attIdx)) <= splittingAtt.numericAttSplit){
                            lessOrEqual.add(item);
                        }else{
                            greater.add(item);
                        }
                    }
                    Node lessOrEqualSubtree = c45(lessOrEqual, A, threshold, classVar);
                    Node greaterSubtree = c45(greater, A, threshold, classVar);
                    tree.addEdge(Double.toString(splittingAtt.numericAttSplit), lessOrEqualSubtree, "le");
                    tree.addEdge(Double.toString(splittingAtt.numericAttSplit), greaterSubtree, "gt");

                }
                return tree;
            }
        }
    }

    public static SplittingAttPair selectSplittingAttribute(List<ArrayList<String>> D, 
    ArrayList<ArrayList<String>> A, double threshold, Integer classVarLoc){
        double p0 = baseEntropy(D, A, classVarLoc);
        HashMap<Integer, Double> gains = new HashMap<>();
        HashMap<Integer, Double> splitGains = new HashMap<>();
        for(int i = 0; i < A.get(0).size(); i++){
            if(i == classVarLoc || A.get(2).get(i).equals("0")){
                continue;
            }
            if(!A.get(1).get(i).equals("0")){ //categorical
                gains.put(i, p0 - attEntropy(D, A, classVarLoc, i));
            } else{ //numeric
                DoublePair bestBinarySplit = findBestSplit(A, D, p0, i, classVarLoc);
                gains.put(i, bestBinarySplit.gain);
                splitGains.put(i, bestBinarySplit.split);
            }
            //gains.put(i, p0 - attEntropy(D, A, threshold, classVarLoc, i));
        }
        double maxGain = -1;
        int winningIdx = -1;
        for(Map.Entry<Integer, Double> set : gains.entrySet()){
            if(set.getValue() > maxGain){
                maxGain = set.getValue();
                winningIdx = set.getKey();
            }
        }
        if(winningIdx == -1){
            return null;
        }
        else if(gains.get(winningIdx) > threshold){
            if(splitGains.get(winningIdx) == null){
                return new SplittingAttPair(winningIdx, 0);
            }else{
                return new SplittingAttPair(winningIdx, splitGains.get(winningIdx));
            }
        } else{
            return null;
        }
    }

    public static DoublePair findBestSplit(ArrayList<ArrayList<String>> A, 
    List<ArrayList<String>> D, double p0, int attIdx, int classVarLoc){
        // System.out.println(attIdx);
        HashMap<Double, Double> splitGains = new HashMap<>();
        List<ArrayList<String>> sortedD = new ArrayList<>();
        for(ArrayList<String> item : D){
            sortedD.add(item);
        }
        sortedD.sort((ArrayList<String> a, ArrayList<String> b) -> {
            if(Double.valueOf(a.get(attIdx)) > Double.valueOf(b.get(attIdx))){
                return 1;
            } else if(Double.valueOf(a.get(attIdx)) < Double.valueOf(b.get(attIdx))){
                return -1;
            } else{
                return 0;
            }
        });
        for(ArrayList<String> item : sortedD){
            if(splitGains.get(Double.valueOf(item.get(attIdx))) == null){
                splitGains.put(Double.valueOf(item.get(attIdx)), p0 - numericAttSplitEntropy(A, sortedD, classVarLoc, attIdx, Double.valueOf(item.get(attIdx))));
            }
        }
        double maxGain = -1;
        Double winningSplit = null;
        for(Map.Entry<Double, Double> set : splitGains.entrySet()){
            if(set.getValue() > maxGain){
                maxGain = set.getValue();
                winningSplit = set.getKey();
            }
        }
        return new DoublePair(winningSplit, maxGain);
    }

    public static Pair popularityContest(List<ArrayList<String>> D, 
    ArrayList<ArrayList<String>> A, int classVarLoc){
        HashMap<String, Integer> score = new HashMap<>();
        for(ArrayList<String> point : D){
            if(score.get(point.get(classVarLoc)) == null){
                score.put(point.get(classVarLoc), 1);
            } else {
                score.put(point.get(classVarLoc), score.get(point.get(classVarLoc)) + 1);
            }
        }
        int frontRunner = -1;
        String leadingAtt = null;
        for(Map.Entry<String, Integer> set : score.entrySet()){
            if(set.getValue() > frontRunner){
                frontRunner = set.getValue();
                leadingAtt = set.getKey();
            }
        }
        return new Pair(leadingAtt, Double.valueOf(frontRunner));
    }

    public static double log(double num){
        return Math.log(num)/Math.log(2);
    }

    public static double baseEntropy(List<ArrayList<String>> D,
    ArrayList<ArrayList<String>> arrayList, Integer classVarLoc){
        HashMap<String, Integer> score = new HashMap<>();
        for(ArrayList<String> point : D){
            if(score.get(point.get(classVarLoc)) == null){
                score.put(point.get(classVarLoc), 1);
            } else {
                score.put(point.get(classVarLoc), score.get(point.get(classVarLoc)) + 1);
            }
        }
        double entropy = 0.0;
        for(Map.Entry<String, Integer> set : score.entrySet()){
            double probability = Double.valueOf(set.getValue()) / Double.valueOf(D.size());
            entropy += (probability * log(1/probability));
        }
        return entropy;
    }

    public static double attEntropy(List<ArrayList<String>> D,
    ArrayList<ArrayList<String>> A, Integer classVarLoc, int attIdx){
        HashMap<String, ArrayList<ArrayList<String>>> splits = new HashMap<>();
        for(ArrayList<String> point : D){
            if(splits.get(point.get(attIdx)) == null){
                ArrayList<ArrayList<String>> temp = new ArrayList<>();
                splits.put(point.get(attIdx), temp);
            }
            ArrayList<ArrayList<String>> temp = splits.get(point.get(attIdx));
            temp.add(point);
            splits.put(point.get(attIdx), temp);
        }
        double entropy = 0.0;
        for(Map.Entry<String, ArrayList<ArrayList<String>>> set : splits.entrySet()){
            double probability = Double.valueOf(set.getValue().size()) / Double.valueOf(D.size());
            entropy += (probability * baseEntropy(set.getValue(), A, classVarLoc));
        }
        return entropy;
    }

    public static double numericAttSplitEntropy(ArrayList<ArrayList<String>> A, 
    List<ArrayList<String>> sortedD, Integer classVarLoc, int attIdx, double split){
        List<ArrayList<String>> lessOrEqual = new ArrayList<>();
        List<ArrayList<String>> greater = new ArrayList<>();
        for(ArrayList<String> item : sortedD){
            if(Double.valueOf(item.get(attIdx)) <= split){
                lessOrEqual.add(item);
            } else{
                greater.add(item);
            }
        }
        double probLessOrEqual = Double.valueOf(lessOrEqual.size()) / Double.valueOf(sortedD.size());
        double probGreater = Double.valueOf(greater.size()) / Double.valueOf(sortedD.size());
        double entropy = (probLessOrEqual * baseEntropy(lessOrEqual, A, classVarLoc)) + 
            (probGreater * baseEntropy(greater, A, classVarLoc));
        return entropy;
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

    public static ArrayList<String> readRestrictions(String path){
        Scanner sc;
        ArrayList<String> restrictions = new ArrayList<>();
        try {
            sc = new Scanner(new File(path));
            while (sc.hasNextLine()){
                ArrayList<String> lineVals = new ArrayList<>();
                String[] line = sc.nextLine().split(",");
                for(String s : line){
                    lineVals.add(s);
                }
                restrictions = lineVals;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return restrictions;
    }

    public static JSONObject createJSON(Node tree, String dataSetFile){
        JSONObject json = new JSONObject();
        try {
            json.put("dataset", dataSetFile);
            if(tree.edges != null){
                json.put("node", tree.toJSON());
            } else{
                json.put("leaf", tree.toJSON());
            }
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static void printJSON(Node tree, String dataSetFile){
        JSONObject json = new JSONObject();
        FileWriter outputFile = null;
        try {
            json.put("dataset", dataSetFile);
            if(tree.edges != null){
                json.put("node", tree.toJSON());
            } else{
                json.put("leaf", tree.toJSON());
            }
            // System.out.println(json.toString(4));
            outputFile = new FileWriter("output.json");
            outputFile.write(json.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            try{
                outputFile.flush();
                outputFile.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}

class Node{
    String attribute;
    String decision;
    ArrayList<Edge> edges;
    double p;
    public Node(String a, String d, ArrayList<Edge> l, double p){
        if(a != ""){
            this.attribute = a;
        }
        if(d != ""){
            this.decision = d;
        }
        if(l != null){
            this.edges = l;
        }
        this.p=p;
    }

    public void addEdge(String label, Node subtree, String direction){
        this.edges.add(new Edge(label, subtree, direction));
    }

    public String toString(){
        if(edges == null){
            return "\nLeaf: " + decision;
        } else{
            return "Node: " + attribute + "\nedges: " + edges.toString();
        }
    }

    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        try {
            if(edges == null){
                JSONObject leafJSON = new JSONObject();
                leafJSON.put("decision", decision);
                leafJSON.put("p", p);
                return leafJSON;
            } else{
                JSONObject nodeJSON = new JSONObject();
                nodeJSON.put("var", attribute);
                JSONArray edgesArr = new JSONArray();
                for(Edge e : edges){
                    edgesArr.put(e.toJSON());
                }
                nodeJSON.put("edges", edgesArr);
                return nodeJSON;
            }
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}

class Edge{
    String edge;
    Node next;
    String direction;
    public Edge(String e, Node n, String d){
        this.edge = e;
        this.next = n;
        this.direction = d;
    }

    public String toString(){
        return "\n" + edge + " direction: " + direction + " node: " + next.toString();
    }

    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        try {
            JSONObject edgeJSON = new JSONObject();
            edgeJSON.put("value", edge);
            if(!direction.equals("")){
                edgeJSON.put("direction", direction);
            }
            if(next.edges != null){
                edgeJSON.put("node", next.toJSON());
            } else{
                edgeJSON.put("leaf", next.toJSON());
            }
            json.put("edge", edgeJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}

class Pair{
    String att;
    double popularity;

    public Pair(String a, double p){
        this.att=a;
        this.popularity=p;
    }
}

class DoublePair{
    double split;
    double gain;

    public DoublePair(double s, double g){
        this.split=s;
        this.gain=g;
    }
}

class SplittingAttPair{
    int attIdx;
    double numericAttSplit;

    public SplittingAttPair(int i, double s){
        this.attIdx=i;
        this.numericAttSplit = s;
    }
}
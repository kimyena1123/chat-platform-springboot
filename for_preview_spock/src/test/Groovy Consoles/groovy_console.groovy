import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction
import java.util.function.Function
import java.util.stream.IntStream

//print
println("hello")
println "hello"
println 'hello'

//array
int[] javaArray = new int[]{1,2,3,4,5};
def groovyArray = [1,2,3,4,5] as int[]

println javaArray
println groovyArray
println javaArray.equals(groovyArray)   //true
println javaArray == groovyArray        //true
println javaArray.is(groovyArray)       //false

//list
List<Integer> javaList = List.of(1,2,3,4,5);
var javaList2 = List.of(1,2,3,4,5); //정적 타입추론
List<Integer> groovyList = [1,2,3,4,5]
def groovyList2 = [1,2,3,4,5] //동적 타입추론
println javaList
println javaList2

println groovyList
println groovyList2

println groovyList.getClass(); //class java.util.ArrayList
println groovyList2.getClass(); //class java.util.ArrayList

println javaList.get(0) //1
println javaList.get(4) //5

println groovyList[0] //1
println groovyList[4] //5
println groovyList[-1] //5
println groovyList[-5] //1

//Map
Map<String, Object> javaMap = Map.of("name", "yena", "age", 20);
println javaMap.get("name")
println javaMap.get("age")

def groovyMap = [name: "yena", age: 20]
println groovyMap.get("name")
println groovyMap.get("age")

println groovyMap.getClass() //class java.util.LinkedHashMap

def cMap = new ConcurrentHashMap([name: 'yena', age: 20])
println cMap.getClass() //class java.util.concurrent.ConcurrentHashMap

def cMap2 = [name: 'yena', age: 20] as ConcurrentHashMap
println cMap2.getClass() //class java.util.concurrent.ConcurrentHashMap

//string format
String word = "world";
println String.format("hello + %s", word)   //hello + world

println "hello + ${word}"                   //hello + world
println "hello + $word"                     //hello + world
println 'hello + $word'                     //hello + $word

// range
IntStream.rangeClosed(1,10).forEach (value -> System.out.println(value))
println()
(1..10).each {value -> println value}

// if
def value = 20
if(value in 10..30){ // value의 값이 10부터 30 사이라면
    println "$value is between 10~20"
}

// for
for (int i = 0; i < 5; i++){ //
    println "idx : " + i //0,1,2,3,4
}
println()

for(i in 0..< 5){ //5 미포함
    println "idx : " + i //0,1,2,3,4
}
println()

for(i in 0..5){ //5 포함
    println "idx : " + i //0,1,2,3,4,5
}
println()

// foreach
var jList = Arrays.asList(1,2,3)
for(Integer num: jList){
    println num
}

for (def num : [1,2,3]){
    println num
}


// switch
int val = 12;
var jResult = switch (val){
    case 10, 11,12, 13, 14 -> {
        println "java >> switch 내에서의 value : $val"
        yield val * 2
    }
    default -> 0
}
println jResult

def gResult = switch(val){
    case 10..14 -> {
        println "groovy >> switch 내에서의 value : $val"
        val * 2
    }
    default -> 0
}
println gResult

def res = 'ok'
switch(res){
    case ~/ok|yes|sure/ -> {
        println 'positive'
    }
    default -> 'none'
}


// null, empty, ''은 FALSE 처리
"".isEmpty() ? println("empty") : println('none empty') //empty
"" ? println('none empty') : println("empty") //순서가 위랑 반대여야 같은 결과가 나온다.

var list = Collections.emptyList();
list.isEmpty() ? println("empty") : println('none empty') //empty
list ? println('none empty') : println("empty") //empty

var obj = null
obj == null ? println("null") : println('none null') //null
obj ? println('none null') : println("null") //null


//null safe
class Person{
    String name;
}

//?. >> 평가하는 값이 null이면 호출x
//?: >> 평가흐는 값이 null이면 호출O
Person person = null;
println person != null ? person.name : "invalid" //java ver : invalid
println person ?.name ?: 'invalid'//?. : person이 null이 아닐 때만 name을 호출한다.

println(5 + 2)          // 7
println 5.plus(2) // 7

//lambda와 클로저
BiFunction<Long, Long, Long> jSum = (Long a, Long b) -> a, b;
println jSum.apply(5,5) //5

def gSum = { x, y -> x + y }
println gSum(5, 5) // 10


// 함수 합성
Function<Long, Long> addFive = a -> a + 5
Function<Long, Long> mulByTwo = a -> a * 2
Function<Long, Long> combined = addFive.andThen(mulByTwo)
println ("java ver Res : " + combined.apply(2)) // 14

def addFive2 = {a -> a + 5}
def mulByTwo2 = {a -> a * 2}
def combined2 = addFive2 >> mulByTwo2
println ("groovy ver Res : " + combined2(2))


// 커링
//자바는 커링을 지원 X
Function<Long, Function<Long, Long>> jSunForCurry = a -> (Long c) -> a + c;
Function<Long, Long> jAddFive = jSunForCurry(5)
println jAddFive.apply(5) //10

def gSumAB = { num1, num2 -> num1 + num2}
def gSumB = gSumAB.curry(5)
println gSumB(5) //10



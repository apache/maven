

using System;
using System.Text;

namespace Com.Test {
	public class Test2 {
 	  	public int MyProp {
   			get { return 1; }
   		}

   		[STAThread]
   		public static void Main(string[] args) {
 System.Console.WriteLine("HELLO WORLD");
   		}
	}
}

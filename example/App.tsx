import React, { useState } from 'react';
import {Text, View, StyleSheet, Button, Alert } from 'react-native';
import { NitroMetamask } from '@novastera-oss/nitro-metamask';

function App(): React.JSX.Element {
  const [result, setResult] = useState<string>('');

  const handleConnect = async () => {
    try {
      const connectResult = await NitroMetamask.connect();
      setResult(`Connected: ${connectResult.address} (Chain: ${connectResult.chainId})`);
      Alert.alert('Success', `Connected to ${connectResult.address}`);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      setResult(`Error: ${errorMessage}`);
      Alert.alert('Error', errorMessage);
    }
  };

  const handleSign = async () => {
    try {
      const message = 'Hello from Nitro MetaMask!';
      const signature = await NitroMetamask.signMessage(message);
      setResult(`Signed: ${signature.substring(0, 20)}...`);
      Alert.alert('Success', 'Message signed successfully');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : String(error);
      setResult(`Error: ${errorMessage}`);
      if (errorMessage.includes('No connected account') || errorMessage.includes('Call connect() first')) {
        Alert.alert('Not Connected', 'Please connect to MetaMask first by tapping "Connect MetaMask"');
      } else {
        Alert.alert('Error', errorMessage);
      }
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Nitro MetaMask Example</Text>
      <View style={styles.buttonContainer}>
        <Button title="Connect MetaMask" onPress={handleConnect} />
      </View>
      <View style={styles.buttonContainer}>
        <Button title="Sign Message" onPress={handleSign} />
      </View>
      {result ? (
        <Text style={styles.result}>{result}</Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 30,
    color: '#333',
  },
  buttonContainer: {
    marginVertical: 10,
    width: '100%',
    maxWidth: 300,
  },
  result: {
    marginTop: 20,
    padding: 10,
    backgroundColor: '#f0f0f0',
    borderRadius: 5,
    fontSize: 12,
    color: '#666',
    textAlign: 'center',
  },
});

export default App;
